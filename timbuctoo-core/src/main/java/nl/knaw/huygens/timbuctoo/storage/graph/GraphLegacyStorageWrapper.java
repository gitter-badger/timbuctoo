package nl.knaw.huygens.timbuctoo.storage.graph;

import java.util.Date;
import java.util.List;
import java.util.Set;

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
import nl.knaw.huygens.timbuctoo.storage.UpdateException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class GraphLegacyStorageWrapper implements Storage {

  private static final Class<Relation> RELATION_TYPE = Relation.class;
  private final GraphStorage graphStorage;
  private final IdGenerator idGenerator;

  @Inject
  public GraphLegacyStorageWrapper(GraphStorage graphStorage) {
    this(graphStorage, new IdGenerator());
  }

  public GraphLegacyStorageWrapper(GraphStorage graphStorage, IdGenerator idGenerator) {
    this.graphStorage = graphStorage;
    this.idGenerator = idGenerator;
  }

  @Override
  public void createIndex(boolean unique, Class<? extends Entity> type, String... fields) throws StorageException {
    // FIXME indexes should be created in a different way for Neo4J TIM-109
    //    throw new UnsupportedOperationException("Yet to be implemented");

  }

  @Override
  public <T extends Entity> String getStatistics(Class<T> type) {
    // FIXME TIM-121 What kind of information do we want to show?
    // We cannot reproduce information similar to Mongo's.
    return "";
  }

  @Override
  public void close() {
    graphStorage.close();
  }

  @Override
  public boolean isAvailable() {
    return graphStorage.isAvailable();
  }

  @Override
  public <T extends SystemEntity> String addSystemEntity(Class<T> type, T entity) throws StorageException {
    String id = addAdministrativeValues(type, entity);
    graphStorage.addSystemEntity(type, entity);
    return id;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends DomainEntity> String addDomainEntity(Class<T> type, T entity, Change change) throws StorageException {
    removePID(entity);
    String id = addAdministrativeValues(type, entity);

    if (RELATION_TYPE.isAssignableFrom(type)) {
      graphStorage.addRelation((Class<? extends Relation>) type, (Relation) entity, change);
    } else {
      graphStorage.addDomainEntity(type, entity, change);
    }

    return id;
  }

  /**
   * Adds the administrative values to the entity.
   * @param type the type to generate the id for
   * @param entity the entity to add the values to
   * @return the generated id
   */
  private <T extends Entity> String addAdministrativeValues(Class<T> type, T entity) {
    String id = idGenerator.nextIdFor(type);
    Change change = Change.newInternalInstance();

    entity.setCreated(change);
    entity.setModified(change);
    entity.setId(id);
    updateRevision(entity);

    return id;
  }

  private <T extends DomainEntity> void removePID(T entity) {
    entity.setPid(null);
  }

  private <T extends Entity> void updateAdministrativeValues(T entity) {
    entity.setModified(Change.newInternalInstance());
    updateRevision(entity);
  }

  private <T extends Entity> void updateRevision(T entity) {
    int rev = entity.getRev();
    entity.setRev(++rev);
  }

  @Override
  public <T extends SystemEntity> void updateSystemEntity(Class<T> type, T entity) throws StorageException {
    updateAdministrativeValues(entity);
    graphStorage.updateSystemEntity(type, entity);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends DomainEntity> void updateDomainEntity(Class<T> type, T entity, Change change) throws StorageException {
    removePID(entity);
    updateAdministrativeValues(entity);
    if (RELATION_TYPE.isAssignableFrom(type)) {
      graphStorage.updateRelation((Class<? extends Relation>) type, (Relation) entity, change);
    } else {
      if (baseTypeExists(type, entity) && variantExists(type, entity)) {
        graphStorage.updateDomainEntity(type, entity, change);
      } else if (baseTypeExists(type, entity)) {
        graphStorage.addVariant(type, entity, change);
      } else {
        throw new UpdateException(String.format("%s with id %s does not exist.", type, entity.getId()));
      }
    }
  }

  private <T extends DomainEntity> boolean variantExists(Class<T> type, T entity) {
    return graphStorage.entityExists(type, entity.getId());
  }

  private <T extends DomainEntity> boolean baseTypeExists(Class<T> type, T entity) {
    return graphStorage.entityExists(TypeRegistry.getBaseClass(type), entity.getId());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends DomainEntity> void setPID(Class<T> type, String id, String pid) throws StorageException {
    if (RELATION_TYPE.isAssignableFrom(type)) {
      graphStorage.setRelationPID((Class<? extends Relation>) type, id, pid);
    } else {
      graphStorage.setDomainEntityPID(type, id, pid);
    }
  }

  @Override
  public <T extends SystemEntity> int deleteSystemEntity(Class<T> type, String id) throws StorageException {
    return graphStorage.deleteSystemEntity(type, id);
  }

  @Override
  public <T extends SystemEntity> int deleteSystemEntities(Class<T> type) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends SystemEntity> int deleteByModifiedDate(Class<T> type, Date dateValue) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends DomainEntity> void deleteDomainEntity(Class<T> type, String id, Change change) throws StorageException {
    graphStorage.deleteDomainEntity(type, id, change);
  }

  // FIXME let this method find the non persistent and delete them. See TIM-145.
  @Override
  public <T extends DomainEntity> void deleteNonPersistent(Class<T> type, List<String> ids) throws StorageException {
    if (RELATION_TYPE.isAssignableFrom(type)) {
      return;
    }
    Change change = Change.newInternalInstance();
    for (String id : ids) {
      graphStorage.deleteDomainEntity(TypeRegistry.toBaseDomainEntity(type), id, change);
    }
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

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Entity> boolean entityExists(Class<T> type, String id) throws StorageException {
    if (RELATION_TYPE.isAssignableFrom(type)) {
      return graphStorage.relationExists((Class<? extends Relation>) type, id);
    } else {
      return graphStorage.entityExists(type, id);
    }
  }

  @Override
  public <T extends Entity> T getEntityOrDefaultVariation(Class<T> type, String id) throws StorageException {
    if (graphStorage.entityExists(type, id)) {
      return graphStorage.getEntity(type, id);
    } else {
      return graphStorage.getDefaultVariation(type, id);
    }
  }

  @Override
  public <T extends Entity> T getEntity(Class<T> type, String id) throws StorageException {
    if (RELATION_TYPE.isAssignableFrom(type)) {
      @SuppressWarnings("unchecked")
      T relationDomainEntity = (T) graphStorage.getRelation((Class<Relation>) type, id);
      return relationDomainEntity;
    } else {
      return graphStorage.getEntity(type, id);
    }

  }

  @Override
  public <T extends SystemEntity> StorageIterator<T> getSystemEntities(Class<T> type) throws StorageException {
    return graphStorage.getEntities(type);
  }

  @Override
  public <T extends DomainEntity> StorageIterator<T> getDomainEntities(Class<T> type) throws StorageException {
    return graphStorage.getEntities(type);
  }

  @Override
  public <T extends Entity> StorageIterator<T> getEntitiesByProperty(Class<T> type, String field, String value) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Entity> long count(Class<T> type) {
    if (RELATION_TYPE.isAssignableFrom(type)) {
      return graphStorage.countRelations((Class<? extends Relation>) type);
    } else {
      return graphStorage.countEntities(type);
    }
  }

  @Override
  public <T extends Entity> T findItemByProperty(Class<T> type, String field, String value) throws StorageException {
    if (RELATION_TYPE.isAssignableFrom(type)) {
      @SuppressWarnings("unchecked")
      T relation = (T) graphStorage.findRelationByProperty((Class<? extends Relation>) type, field, value);
      return relation;
    } else {
      return graphStorage.findEntityByProperty(type, field, value);
    }
  }

  @Override
  public <T extends DomainEntity> List<T> getAllVariations(Class<T> type, String id) throws StorageException {
    return graphStorage.getAllVariations(type, id);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends DomainEntity> T getRevision(Class<T> type, String id, int revision) throws StorageException {
    if (RELATION_TYPE.isAssignableFrom(type)) {
      return (T) graphStorage.getRelationRevision((Class<? extends Relation>) type, id, revision);
    } else {
      return graphStorage.getDomainEntityRevision(type, id, revision);
    }
  }

  @Override
  public <T extends DomainEntity> List<T> getAllRevisions(Class<T> type, String id) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends Relation> T findRelation(Class<T> type, String sourceId, String targetId, String relationTypeId) throws StorageException {
    return graphStorage.findRelation(type, sourceId, targetId, relationTypeId);
  }

  @Override
  public <T extends Relation> StorageIterator<T> findRelations(Class<T> type, String sourceId, String targetId, String relationTypeId) throws StorageException {
    throw new UnsupportedOperationException("Yet to be implemented");
  }

  @Override
  public <T extends Relation> StorageIterator<T> getRelationsByEntityId(Class<T> type, String id) throws StorageException {
    return graphStorage.getRelationsByEntityId(type, id);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends DomainEntity> List<String> getAllIdsWithoutPIDOfType(Class<T> type) throws StorageException {
    if (RELATION_TYPE.isAssignableFrom(type)) {
      return graphStorage.getIdsOfNonPersistentRelations((Class<Relation>) type);
    } else {
      return graphStorage.getIdsOfNonPersistentDomainEntities(type);
    }
  }

  @Override
  public List<String> getRelationIds(List<String> ids) throws StorageException {
    Set<String> relationIds = Sets.newHashSet();
    for (String id : ids) {
      StorageIterator<Relation> iterator = graphStorage.getRelationsByEntityId(RELATION_TYPE, id);

      for (; iterator.hasNext();) {
        relationIds.add(iterator.next().getId());
      }

    }

    return Lists.newArrayList(relationIds);
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