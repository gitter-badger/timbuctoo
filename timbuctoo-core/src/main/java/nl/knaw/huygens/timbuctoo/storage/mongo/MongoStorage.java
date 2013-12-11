package nl.knaw.huygens.timbuctoo.storage.mongo;

import static nl.knaw.huygens.timbuctoo.config.TypeNames.getInternalName;
import static nl.knaw.huygens.timbuctoo.config.TypeRegistry.toBaseDomainEntity;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import nl.knaw.huygens.timbuctoo.config.Configuration;
import nl.knaw.huygens.timbuctoo.config.TypeRegistry;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.model.EntityRef;
import nl.knaw.huygens.timbuctoo.model.Reference;
import nl.knaw.huygens.timbuctoo.model.Relation;
import nl.knaw.huygens.timbuctoo.model.RelationType;
import nl.knaw.huygens.timbuctoo.model.SystemEntity;
import nl.knaw.huygens.timbuctoo.model.util.Change;
import nl.knaw.huygens.timbuctoo.storage.EmptyStorageIterator;
import nl.knaw.huygens.timbuctoo.storage.EntityInducer;
import nl.knaw.huygens.timbuctoo.storage.EntityReducer;
import nl.knaw.huygens.timbuctoo.storage.Storage;
import nl.knaw.huygens.timbuctoo.storage.StorageException;
import nl.knaw.huygens.timbuctoo.storage.StorageIterator;

import org.mongojack.DBQuery;
import org.mongojack.internal.stream.JacksonDBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteResult;

public class MongoStorage implements Storage {

  private static final Logger LOG = LoggerFactory.getLogger(MongoStorage.class);

  private final TypeRegistry typeRegistry;
  private final MongoDB mongoDB;
  private final EntityIds entityIds;

  private MongoQueries queries;
  private ObjectMapper objectMapper;
  private TreeEncoderFactory treeEncoderFactory;
  private TreeDecoderFactory treeDecoderFactory;
  private EntityInducer inducer;
  private EntityReducer reducer;

  @Inject
  public MongoStorage(TypeRegistry registry, Configuration config) throws UnknownHostException, MongoException {
    typeRegistry = registry;
    MongoOptions options = new MongoOptions();
    options.safe = true;

    String host = config.getSetting("database.host", "localhost");
    int port = config.getIntSetting("database.port", 27017);
    Mongo mongo = new Mongo(new ServerAddress(host, port), options);

    String dbName = config.getSetting("database.name");
    DB db = mongo.getDB(dbName);

    String user = config.getSetting("database.user");
    if (!user.isEmpty()) {
      String password = config.getSetting("database.password");
      db.authenticate(user, password.toCharArray());
    }

    mongoDB = new MongoDB(mongo, db);
    entityIds = new EntityIds(db, typeRegistry);

    initialize();
    ensureIndexes();
  }

  @VisibleForTesting
  MongoStorage(TypeRegistry registry, Mongo mongo, DB db, EntityIds entityIds) {
    this.typeRegistry = registry;
    this.mongoDB = new MongoDB(mongo, db);
    this.entityIds = entityIds;

    initialize();
  }

  @VisibleForTesting
  MongoStorage(TypeRegistry registry, MongoDB mongoDB, EntityIds entityIds) {
    this.typeRegistry = registry;
    this.mongoDB = mongoDB;
    this.entityIds = entityIds;

    initialize();
  }

  private void initialize() {
    queries = new MongoQueries();
    objectMapper = new ObjectMapper();
    treeEncoderFactory = new TreeEncoderFactory(objectMapper);
    treeDecoderFactory = new TreeDecoderFactory();
    inducer = new EntityInducer();
    reducer = new EntityReducer(typeRegistry);
  }

  private void ensureIndexes() {
    DBCollection collection = getDBCollection(Relation.class);
    collection.ensureIndex(new BasicDBObject("^sourceId", 1));
    collection.ensureIndex(new BasicDBObject("^targetId", 1));
    collection.ensureIndex(new BasicDBObject("^sourceId", 1).append("^targetId", 1));
  }

  @Override
  public void close() {
    mongoDB.close();
  }

  // --- support -------------------------------------------------------

  private final Map<Class<? extends Entity>, DBCollection> collectionCache = Maps.newHashMap();

  private <T extends Entity> DBCollection getDBCollection(Class<T> type) {
    DBCollection collection = collectionCache.get(type);
    if (collection == null) {
      Class<? extends Entity> baseType = typeRegistry.getBaseClass(type);
      String collectionName = getInternalName(baseType);
      collection = mongoDB.getCollection(collectionName);
      collection.setDBDecoderFactory(treeDecoderFactory);
      collection.setDBEncoderFactory(treeEncoderFactory);
      collectionCache.put(type, collection);
    }
    return collection;
  }

  private <T extends Entity> DBCollection getVersionCollection(Class<T> type) {
    Class<? extends Entity> baseType = typeRegistry.getBaseClass(type);
    String collectionName = getInternalName(baseType) + "_versions";
    DBCollection collection = mongoDB.getCollection(collectionName);
    collection.setDBDecoderFactory(treeDecoderFactory);
    collection.setDBEncoderFactory(treeEncoderFactory);
    return collection;
  }

  private DBObject toDBObject(JsonNode node) {
    return new JacksonDBObject<JsonNode>(node, JsonNode.class);
  }

  @SuppressWarnings("unchecked")
  private JsonNode toJsonNode(DBObject object) throws IOException {
    if (object instanceof JacksonDBObject) {
      return (((JacksonDBObject<JsonNode>) object).getObject());
    } else if (object instanceof DBJsonNode) {
      return ((DBJsonNode) object).getDelegate();
    } else {
      LOG.error("Failed to convert {}", object.getClass());
      throw new IOException("Unknown DBObject type");
    }
  }

  // --- generic storage layer -----------------------------------------

  private DBObject findExisting(Class<? extends Entity> type, DBObject query) throws IOException {
    DBObject dbObject = getDBCollection(type).findOne(query);
    if (dbObject == null) {
      LOG.error("No match for query {}", query);
      throw new IOException("No match");
    }
    return dbObject;
  }

  private <T extends Entity> T getItem(Class<T> type, DBObject query) throws IOException {
    DBObject item = getDBCollection(type).findOne(query);
    return (item != null) ? reducer.reduceVariation(type, toJsonNode(item), null) : null;
  }

  private <T extends Entity> StorageIterator<T> getItems(Class<T> type, DBObject query) {
    DBCursor cursor = getDBCollection(type).find(query);
    return (cursor != null) ? new MongoStorageIterator<T>(type, cursor, reducer) : new EmptyStorageIterator<T>();
  }

  private <T extends Entity> int removeItem(Class<T> type, DBObject query) {
    // LOG.debug("Query: {}", objectMapper.valueToTree(query));
    WriteResult result = getDBCollection(type).remove(query);
    return (result != null) ? result.getN() : 0;
  }

  public <T extends Entity> long count(Class<T> type) {
    Class<? extends Entity> baseType = typeRegistry.getBaseClass(type);
    return getDBCollection(baseType).count();
  }

  // --- entities ------------------------------------------------------

  @Override
  public <T extends Entity> T getItem(Class<T> type, String id) throws IOException {
    DBObject query = queries.selectById(id);
    return getItem(type, query);
  }

  @Override
  public <T extends Entity> StorageIterator<T> getAllByType(Class<T> type) {
    DBObject query = queries.selectAll();
    return getItems(type, query);
  }

  @Override
  public <T extends SystemEntity> String addSystemEntity(Class<T> type, T entity) throws IOException {
    Change change = Change.newInstance();
    String id = entityIds.getNextId(type);

    entity.setId(id);
    entity.setRev(1);
    entity.setCreated(change);
    entity.setModified(change);

    JsonNode tree = inducer.induceSystemEntity(type, entity);
    mongoDB.insert(getDBCollection(type), id, toDBObject(tree));

    return id;
  }

  @Override
  public <T extends DomainEntity> String addDomainEntity(Class<T> type, T entity) throws IOException {
    Change change = Change.newInstance();
    String id = entityIds.getNextId(type);

    entity.setId(id);
    entity.setRev(1);
    entity.setCreated(change);
    entity.setModified(change);

    entity.setPid(null);
    entity.setDeleted(false);
    entity.setVariations(null); // make sure the list is empty
    entity.addVariation(typeRegistry.getBaseClass(type));
    entity.addVariation(type);

    JsonNode tree = inducer.induceDomainEntity(type, entity);
    mongoDB.insert(getDBCollection(type), id, toDBObject(tree));

    addInitialVersion(type, id, tree);

    return id;
  }

  private <T extends Entity> void addInitialVersion(Class<T> type, String id, JsonNode actualVersion) throws IOException {
    ArrayNode versionsNode = objectMapper.createArrayNode();
    versionsNode.add(actualVersion);

    ObjectNode itemNode = objectMapper.createObjectNode();
    itemNode.put("_id", id);
    itemNode.put("versions", versionsNode);

    mongoDB.insert(getVersionCollection(type), id, toDBObject(itemNode));
  }

  @Override
  public <T extends SystemEntity> void updateSystemEntity(Class<T> type, String id, T entity) throws IOException {
    Change change = Change.newInstance();
    int revision = entity.getRev();
    DBObject query = queries.selectByIdAndRevision(id, revision);

    T storedEntity = getItem(type, query);
    if (storedEntity == null) {
      LOG.error("No entity with id {} and revision {}", id, revision);
      throw new IOException("Update failed");
    }

    storedEntity.setRev(revision + 1);
    storedEntity.setModified(change);

    JsonNode tree = inducer.induceSystemEntity(type, storedEntity);
    inducer.induceSystemEntity(type, entity, (ObjectNode) tree);

    mongoDB.update(getDBCollection(type), query, toDBObject(tree));
  }

  @Override
  public <T extends DomainEntity> void updateDomainEntity(Class<T> type, String id, T entity) throws IOException {
    Change change = Change.newInstance();
    int revision = entity.getRev();
    DBObject query = queries.selectByIdAndRevision(id, revision);

    JsonNode tree = toJsonNode(findExisting(type, query));

    DomainEntity domainEntity = reducer.reduceExistingVariation(toBaseDomainEntity(type), tree);

    domainEntity.setRev(revision + 1);
    domainEntity.setModified(change);
    domainEntity.setPid(null);
    domainEntity.addVariation(type);

    inducer.adminDomainEntity(domainEntity, (ObjectNode) tree);
    inducer.induceDomainEntity(type, entity, (ObjectNode) tree);

    mongoDB.update(getDBCollection(type), query, toDBObject(tree));

    addVersion(type, id, tree);
  }

  public <T extends DomainEntity> void setPID(Class<T> type, String id, String pid) throws IOException {
    DBObject query = queries.selectById(id);
    DBObject update = queries.setProperty(DomainEntity.PID, pid);
    getDBCollection(type).update(query, update);
  }

  @Override
  public <T extends DomainEntity> void deleteDomainEntity(Class<T> type, String id, Change change) throws IOException {
    DBObject query = queries.selectById(id);

    JsonNode tree = toJsonNode(findExisting(type, query));

    DomainEntity domainEntity = reducer.reduceExistingVariation(toBaseDomainEntity(type), tree);
    int revision = domainEntity.getRev();

    domainEntity.setRev(revision + 1);
    domainEntity.setModified(change);
    domainEntity.setPid(null);
    domainEntity.setDeleted(true);
    domainEntity.setVariations(null); // make sure the list is empty

    inducer.adminDomainEntity(domainEntity, (ObjectNode) tree);
    // TODO remove "real" data

    query = queries.selectByIdAndRevision(id, revision);
    mongoDB.update(getDBCollection(type), query, toDBObject(tree));
  }

  private <T extends Entity> void addVersion(Class<T> type, String id, JsonNode actualVersion) throws IOException {
    ObjectNode versionNode = objectMapper.createObjectNode();
    versionNode.put("versions", actualVersion);

    ObjectNode update = objectMapper.createObjectNode();
    update.put("$push", versionNode);

    mongoDB.update(getVersionCollection(type), queries.selectById(id), toDBObject(update));
  }

  // --- system entities -----------------------------------------------

  @Override
  public <T extends SystemEntity> T findItemByKey(Class<T> type, String key, String value) throws IOException {
    DBObject query = queries.selectByProperty(key, value);
    return getItem(type, query);
  }

  @Override
  public <T extends SystemEntity> T findItem(Class<T> type, T example) throws IOException {
    DBObject query = queries.selectByProperties(type, example);
    return getItem(type, query);
  }

  @Override
  public <T extends SystemEntity> int deleteSystemEntity(Class<T> type, String id) {
    DBObject query = queries.selectById(id);
    return removeItem(type, query);
  }

  @Override
  public <T extends SystemEntity> int deleteAll(Class<T> type) {
    DBObject query = queries.selectAll();
    return removeItem(type, query);
  }

  @Override
  public <T extends SystemEntity> int deleteByDate(Class<T> type, String dateField, Date dateValue) {
    DBObject query = queries.selectByDate(type, dateField, dateValue);
    return removeItem(type, query);
  }

  private RelationType getRelationType(String id) throws IOException {
    DBObject query = queries.selectById(id);
    return getItem(RelationType.class, query);
  }

  // --- domain entities -----------------------------------------------

  @Override
  public <T extends DomainEntity> List<T> getAllVariations(Class<T> type, String id) throws StorageException, IOException {
    DBObject query = queries.selectById(id);
    DBObject item = getDBCollection(type).findOne(query);
    if (item == null) {
      return null;
    }
    List<T> variations = reducer.reduceAllVariations(type, toJsonNode(item));
    for (T variation : variations) {
      addRelationsTo(variation.getClass(), id, variation);
    }
    return variations;
  }

  @Override
  public <T extends DomainEntity> T getVariation(Class<T> type, String id, String variation) throws IOException {
    DBObject query = queries.selectById(id);
    DBObject item = getDBCollection(type).findOne(query);
    return (item != null) ? reducer.reduceVariation(type, toJsonNode(item), variation) : null;
  }

  @Override
  public <T extends DomainEntity> MongoChanges<T> getAllRevisions(Class<T> type, String id) throws IOException {
    DBObject query = queries.selectById(id);
    DBObject item = getVersionCollection(type).findOne(query);
    return (item != null) ? reducer.reduceAllRevisions(type, toJsonNode(item)) : null;
  }

  @Override
  public <T extends DomainEntity> T getRevision(Class<T> type, String id, int revision) throws IOException {
    DBObject query = queries.selectVersionByIdAndRevision(id, revision);
    DBObject dbObject = getVersionCollection(type).findOne(query);
    return (dbObject != null) ? reducer.reduceRevision(type, toJsonNode(dbObject)) : null;
  }

  @Override
  public boolean relationExists(Relation relation) throws IOException {
    DBObject query = queries.selectRelation(relation);
    return getItem(Relation.class, query) != null;
  }

  @Override
  public StorageIterator<Relation> getRelationsOf(Class<? extends DomainEntity> type, String id) throws IOException {
    DBObject query = DBQuery.or(DBQuery.is("^sourceId", id), DBQuery.is("^targetId", id));
    return getItems(Relation.class, query);
  }

  // We retrieve all relations involving the specified entity by its id.
  // Next we need to filter the relations that are compatible with the entity type:
  // a relation is only valid if the entity type we are handling is assignable
  // to the type specified in the relation.
  // For example, if a relation is specified for a DCARArchiver, it is visible when
  // dealing with an entity type DCARArchiver, but not for Archiver.
  //TODO add tests.
  @Override
  public void addRelationsTo(Class<? extends DomainEntity> type, String id, DomainEntity entity) {
    Preconditions.checkNotNull(entity, "entity cannot be null");
    StorageIterator<Relation> iterator = null;
    try {
      iterator = getRelationsOf(type, id); // db access
      while (iterator.hasNext()) {
        Relation relation = iterator.next(); // db access
        RelationType relType = getRelationType(relation.getTypeRef().getId());
        Preconditions.checkNotNull(relType, "Failed to retrieve relation type");
        if (relation.hasSourceId(id)) {
          Class<? extends Entity> cls = typeRegistry.getTypeForIName(relation.getSourceType());
          if (cls != null && cls.isAssignableFrom(type)) {
            Reference reference = relation.getTargetRef();
            entity.addRelation(relType.getRegularName(), getEntityRef(reference)); // db access
          }
        } else if (relation.hasTargetId(id)) {
          Class<? extends Entity> cls = typeRegistry.getTypeForIName(relation.getTargetType());
          if (cls != null && cls.isAssignableFrom(type)) {
            Reference reference = relation.getSourceRef();
            entity.addRelation(relType.getInverseName(), getEntityRef(reference)); // db access
          }
        } else {
          throw new IllegalStateException("Impossible");
        }
      }
    } catch (IOException e) {
      LOG.error("Error while handling {} {}", type.getSimpleName(), id);
    } finally {
      if (iterator != null) {
        iterator.close();
      }
    }
  }

  private EntityRef getEntityRef(Reference reference) throws StorageException, IOException {
    String iname = reference.getType();
    String xname = typeRegistry.getXNameForIName(iname);
    Class<? extends Entity> type = typeRegistry.getTypeForIName(iname);
    Entity entity = getItem(type, reference.getId());

    return new EntityRef(iname, xname, reference.getId(), entity.getDisplayName());
  }

  @Override
  public <T extends DomainEntity> List<String> getAllIdsWithoutPIDOfType(Class<T> type) throws IOException {
    List<String> list = Lists.newArrayList();

    try {
      String variationName = typeRegistry.getIName(type);
      DBObject query = queries.selectVariation(variationName);
      query.put(DomainEntity.PID, new BasicDBObject("$exists", false));
      DBObject columnsToShow = new BasicDBObject("_id", 1);

      DBCursor cursor = getDBCollection(type).find(query, columnsToShow);
      while (cursor.hasNext()) {
        list.add((String) cursor.next().get("_id"));
      }

    } catch (MongoException e) {
      LOG.error("Error while retrieving objects without pid of type {}", type.getSimpleName());
      throw new IOException(e);
    }

    return list;
  }

  @Override
  public List<String> getRelationIds(List<String> ids) throws IOException {
    List<String> relationIds = Lists.newArrayList();

    try {
      DBObject query = DBQuery.or(DBQuery.in("^sourceId", ids), DBQuery.in("^targetId", ids));
      DBObject columnsToShow = new BasicDBObject("_id", 1);

      DBCursor cursor = getDBCollection(Relation.class).find(query, columnsToShow);
      while (cursor.hasNext()) {
        relationIds.add((String) cursor.next().get("_id"));
      }
    } catch (MongoException e) {
      LOG.error("Error while retrieving relation id's of {}", ids);
      throw new IOException(e);
    }

    return relationIds;
  }

  @Override
  public <T extends DomainEntity> void deleteNonPersistent(Class<T> type, List<String> ids) throws IOException {
    try {
      DBObject query = DBQuery.in("_id", ids);
      query.put(DomainEntity.PID, null);
      getDBCollection(type).remove(query);
    } catch (MongoException e) {
      LOG.error("Error while removing entities of type {}", type.getSimpleName());
      throw new IOException(e);
    }
  }

}
