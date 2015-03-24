package nl.knaw.huygens.timbuctoo.storage.neo4j;

import static nl.knaw.huygens.timbuctoo.model.Entity.ID_PROPERTY_NAME;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import nl.knaw.huygens.timbuctoo.config.TypeNames;
import nl.knaw.huygens.timbuctoo.config.TypeRegistry;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.model.Relation;
import nl.knaw.huygens.timbuctoo.model.SystemEntity;
import nl.knaw.huygens.timbuctoo.model.util.Change;
import nl.knaw.huygens.timbuctoo.storage.NoSuchEntityException;
import nl.knaw.huygens.timbuctoo.storage.Storage;
import nl.knaw.huygens.timbuctoo.storage.StorageException;
import nl.knaw.huygens.timbuctoo.storage.StorageIterator;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import com.google.inject.Inject;

public class Neo4JLegacyStorageWrapper implements Storage {

  public static final String RELATIONSHIP_ID_INDEX = "RelationShip id";

  private final GraphDatabaseService db;
  private final Neo4JStorage neo4JStorage;

  @Inject
  public Neo4JLegacyStorageWrapper(GraphDatabaseService db, Neo4JStorage neo4JStorage) {
    this.db = db;
    this.neo4JStorage = neo4JStorage;
  }

  @Override
  public void createIndex(boolean unique, Class<? extends Entity> type, String... fields) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");

  }

  @Override
  public <T extends Entity> String getStatistics(Class<T> type) {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public void close() {
    throw new UnsupportedOperationException("Yet to be implemented");

  }

  @Override
  public <T extends SystemEntity> String addSystemEntity(Class<T> type, T entity) throws StorageException {
    return neo4JStorage.addSystemEntity(type, entity);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends DomainEntity> String addDomainEntity(Class<T> type, T entity, Change change) throws StorageException {
    if (Relation.class.isAssignableFrom(type)) {
      return neo4JStorage.addRelation((Class<? extends Relation>) type, (Relation) entity, change);
    } else {
      return neo4JStorage.addDomainEntity(type, entity, change);
    }
  }

  @Override
  public <T extends SystemEntity> void updateSystemEntity(Class<T> type, T entity) throws StorageException {
    neo4JStorage.updateSystemEntity(type, entity);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends DomainEntity> void updateDomainEntity(Class<T> type, T entity, Change change) throws StorageException {
    if (Relation.class.isAssignableFrom(type)) {
      neo4JStorage.updateRelation((Class<? extends Relation>) type, (Relation) entity, change);
    } else {
      neo4JStorage.updateDomainEntity(type, entity, change);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends DomainEntity> void setPID(Class<T> type, String id, String pid) throws StorageException {
    if (Relation.class.isAssignableFrom(type)) {
      neo4JStorage.setRelationPID((Class<? extends Relation>) type, id, pid);
    } else {
      neo4JStorage.setDomainEntityPID(type, id, pid);
    }
  }

  @Override
  public <T extends SystemEntity> int deleteSystemEntity(Class<T> type, String id) throws StorageException {
    int numDeleted = 0;
    try (Transaction transaction = db.beginTx()) {
      ResourceIterator<Node> nodes = findByProperty(type, ID_PROPERTY_NAME, id);
      numDeleted = deleteEntity(nodes);
      transaction.success();
    }

    return numDeleted;
  }

  private int deleteEntity(ResourceIterator<Node> nodes) {
    int numDeleted = 0;
    for (; nodes.hasNext();) {
      Node node = nodes.next();

      for (Iterator<Relationship> relationships = node.getRelationships().iterator(); relationships.hasNext();) {
        relationships.next().delete();
      }

      node.delete();
      numDeleted++;
    }
    return numDeleted;
  }

  @Override
  public <T extends SystemEntity> int deleteSystemEntities(Class<T> type) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends SystemEntity> int deleteByModifiedDate(Class<T> type, Date dateValue) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  // TODO: Make equal to deleteSystemEntity see TIM-54
  @Override
  public <T extends DomainEntity> void deleteDomainEntity(Class<T> type, String id, Change change) throws StorageException {
    if (!TypeRegistry.isPrimitiveDomainEntity(type)) {
      throw new IllegalArgumentException("Only primitive DomainEntities can be deleted. " + type.getSimpleName() + " is not a primitive DomainEntity.");
    }

    try (Transaction transaction = db.beginTx()) {
      ResourceIterator<Node> foundNodes = findByProperty(type, ID_PROPERTY_NAME, id);
      if (!foundNodes.hasNext()) {
        transaction.failure();
        throw new NoSuchEntityException(type, id);
      }

      deleteEntity(foundNodes);

      transaction.success();
    }
  }

  @Override
  public <T extends DomainEntity> void deleteNonPersistent(Class<T> type, List<String> ids) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");

  }

  @Override
  public void deleteVariation(Class<? extends DomainEntity> type, String id, Change change) throws IllegalArgumentException, NoSuchEntityException, StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");

  }

  @Override
  public void deleteRelationsOfEntity(Class<Relation> type, String id) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");

  }

  @Override
  public void declineRelationsOfEntity(Class<? extends Relation> type, String id) throws IllegalArgumentException, StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");

  }

  @Override
  public <T extends Entity> boolean entityExists(Class<T> type, String id) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends Entity> T getEntityOrDefaultVariation(Class<T> type, String id) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends Entity> T getEntity(Class<T> type, String id) throws StorageException {
    if (Relation.class.isAssignableFrom(type)) {
      @SuppressWarnings("unchecked")
      T relationDomainEntity = (T) neo4JStorage.getRelation((Class<Relation>) type, id);
      return relationDomainEntity;
    } else {
      return neo4JStorage.getEntity(type, id);
    }

  }

  @Override
  public <T extends SystemEntity> StorageIterator<T> getSystemEntities(Class<T> type) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends DomainEntity> StorageIterator<T> getDomainEntities(Class<T> type) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends Entity> StorageIterator<T> getEntitiesByProperty(Class<T> type, String field, String value) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends Entity> long count(Class<T> type) {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends Entity> T findItemByProperty(Class<T> type, String field, String value) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  private <T extends Entity> ResourceIterator<Node> findByProperty(Class<T> type, String propertyName, String id) {
    Label internalNameLabel = DynamicLabel.label(TypeNames.getInternalName(type));
    ResourceIterable<Node> foundNodes = db.findNodesByLabelAndProperty(internalNameLabel, propertyName, id);

    ResourceIterator<Node> iterator = foundNodes.iterator();
    return iterator;
  }

  @Override
  public <T extends DomainEntity> List<T> getAllVariations(Class<T> type, String id) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends DomainEntity> T getRevision(Class<T> type, String id, int revision) throws StorageException {
    if (Relation.class.isAssignableFrom(type)) {
      return (T) neo4JStorage.getRelationRevision((Class<? extends Relation>) type, id, revision);
    } else {
      return neo4JStorage.getDomainEntityRevision(type, id, revision);
    }
  }

  @Override
  public <T extends DomainEntity> List<T> getAllRevisions(Class<T> type, String id) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends Relation> T findRelation(Class<T> type, String sourceId, String targetId, String relationTypeId) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends Relation> StorageIterator<T> findRelations(Class<T> type, String sourceId, String targetId, String relationTypeId) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends Relation> StorageIterator<T> getRelationsByEntityId(Class<T> type, String id) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends DomainEntity> List<String> getAllIdsWithoutPIDOfType(Class<T> type) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public List<String> getRelationIds(List<String> ids) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends Relation> List<T> getRelationsByType(Class<T> type, List<String> relationTypeIds) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public boolean doesVariationExist(Class<? extends DomainEntity> type, String id) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

}