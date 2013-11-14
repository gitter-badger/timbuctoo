package nl.knaw.huygens.timbuctoo.storage.mongo;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import nl.knaw.huygens.timbuctoo.config.TypeRegistry;
import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.model.RelationType;
import nl.knaw.huygens.timbuctoo.model.SystemEntity;
import nl.knaw.huygens.timbuctoo.storage.BasicStorage;

import org.mongojack.JacksonDBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

/**
 * Implementation base for Mongo storage classes.
 */
public abstract class MongoStorageBase implements BasicStorage {

  private static final Logger LOG = LoggerFactory.getLogger(MongoStorageBase.class);

  protected final MongoObjectMapper mongoMapper;
  protected final TypeRegistry typeRegistry;
  protected final MongoQueries queries;
  private final Mongo mongo;
  private final String dbName;
  protected DB db;
  private EntityIds entityIds;

  public MongoStorageBase(TypeRegistry registry, Mongo mongo, DB db, String dbName) {
    this.typeRegistry = registry;
    this.queries = new MongoQueries();
    this.mongo = mongo;
    this.dbName = dbName;
    this.db = db;
    this.entityIds = new EntityIds(db, typeRegistry);
    mongoMapper = new MongoObjectMapper();
  }

  public void empty() {
    db.cleanCursors(true);
    mongo.dropDatabase(dbName);
    db = mongo.getDB(dbName);
    entityIds = new EntityIds(db, typeRegistry);
  }

  public void close() {
    db.cleanCursors(true);
    mongo.close();
    LOG.info("Closed");
  }

  public DB getDB() {
    return db;
  }

  public void resetDB(DB db) {
    this.db = db;
  }

  public void setEntityIds(EntityIds entityIds) {
    this.entityIds = entityIds;
  }

  // --- entities ------------------------------------------------------

  public <T extends Entity> long count(Class<T> type) {
    Class<? extends Entity> baseType = typeRegistry.getBaseClass(type);
    return getCollection(baseType).count();
  }

  // --- system entities -----------------------------------------------

  @Override
  public <T extends SystemEntity> T findItemByKey(Class<T> type, String key, String value) throws IOException {
    DBObject query = queries.selectByProperty(key, value);
    return getCollection(type).findOne(query);
  }

  @Override
  public <T extends SystemEntity> T findItem(Class<T> type, T example) throws IOException {
    Map<String, Object> properties = mongoMapper.mapObject(type, example);
    DBObject query = queries.selectByProperties(properties);
    return getCollection(type).findOne(query);
  }

  @Override
  public <T extends SystemEntity> void removeItem(Class<T> type, String id) {
    DBObject query = queries.selectById(id);
    getCollection(type).remove(query);
  }

  @Override
  public <T extends SystemEntity> int removeAll(Class<T> type) {
    DBObject query = queries.selectAll();
    return getCollection(type).remove(query).getN();
  }

  @Override
  public <T extends SystemEntity> int removeByDate(Class<T> type, String dateField, Date dateValue) {
    DBObject query = queries.selectByDate(dateField, dateValue);
    return getCollection(type).remove(query).getN();
  }

  // TODO decide whether this needs to be cacheable
  protected RelationType getRelationType(String id) {
    DBObject query = queries.selectById(id);
    return getCollection(RelationType.class).findOne(query);
  }

  // --- domain entities -----------------------------------------------

  // TODO

  // --- support -------------------------------------------------------

  protected <T extends Entity> JacksonDBCollection<T, String> getCollection(Class<T> type) {
    return MongoUtils.getCollection(db, type);
  }

  protected <T extends Entity> JacksonDBCollection<MongoChanges<T>, String> getVersioningCollection(Class<T> type) {
    return MongoUtils.getVersioningCollection(db, type);
  }

  /**
   * Sets the id of the specified entity to the next value
   * for the collection in which the entity is stored.
   */
  protected <T extends Entity> void setNextId(Class<T> type, T entity) {
    entity.setId(entityIds.getNextId(type));
  }

}
