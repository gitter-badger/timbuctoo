package nl.knaw.huygens.repository.storage;

import java.util.List;

import nl.knaw.huygens.repository.config.DocTypeRegistry;
import nl.knaw.huygens.repository.model.Document;
import nl.knaw.huygens.repository.model.Reference;
import nl.knaw.huygens.repository.model.Relation;
import nl.knaw.huygens.repository.model.RelationType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RelationManager {

  private static final Logger LOG = LoggerFactory.getLogger(RelationManager.class);

  private final DocTypeRegistry registry;
  private final StorageManager storageManager;

  @Inject
  public RelationManager(DocTypeRegistry registry, StorageManager storageManager) {
    this.registry = registry;
    this.storageManager = storageManager;
  }

  /**
   * Returns the relation type with the specified id,
   * or null if it does not exist.
   */
  public RelationType getRelationType(String id) {
    return storageManager.getDocument(RelationType.class, id);
  }

  /**
   * Returns the relation type with the specified reference,
   * or null if it does not exist.
   */
  public RelationType getRelationType(Reference reference) {
    Preconditions.checkArgument(reference.getType().equals("relationtype"), "got type %s", reference.getType());
    return getRelationType(reference.getId());
  }

  public List<RelationType> getSynonyms(RelationType type) {
    return Lists.newArrayList(type);
  }

  public List<RelationType> getInverses(RelationType type) {
    if (type.isSymmetric()) {
      return Lists.newArrayList(type);
    } else {
      return Lists.newArrayList();
    }
  }

  public RelationBuilder getBuilder() {
    return new RelationBuilder();
  }

  public class RelationBuilder {
    private Relation relation;

    public RelationBuilder() {
      relation = new Relation();
    }

    public RelationBuilder type(Reference ref) {
      relation.setTypeRef(ref);
      return this;
    }

    public RelationBuilder source(Reference ref) {
      relation.setSourceRef(ref);
      return this;
    }

    public RelationBuilder source(Class<? extends Document> typeToken, String id) {
      return source(new Reference(typeToken, id));
    }

    public RelationBuilder target(Reference ref) {
      relation.setTargetRef(ref);
      return this;
    }

    public RelationBuilder target(Class<? extends Document> typeToken, String id) {
      return target(new Reference(typeToken, id));
    }

    public Relation build() {
      if (relation.getTypeRef() == null) {
        LOG.error("Missing relation type ref");
        return null;
      }
      if (relation.getSourceRef() == null) {
        LOG.error("Missing source ref");
        return null;
      }
      if (relation.getTargetRef() == null) {
        LOG.error("Missing target ref");
        return null;
      }
      RelationType relationType = storageManager.getDocument(RelationType.class, relation.getTypeRef().getId());
      if (relationType == null) {
        LOG.error("Unknown relation type {}", relation.getTypeRef().getId());
        return null;
      }
      String iname = relation.getSourceRef().getType();
      Class<? extends Document> actualType = registry.getTypeForIName(iname);
      if (!relationType.getSourceDocType().isAssignableFrom(actualType)) {
        LOG.error("Incompatible source type {}", iname);
        return null;
      }
      iname = relation.getTargetRef().getType();
      actualType = registry.getTypeForIName(iname);
      if (!relationType.getTargetDocType().isAssignableFrom(actualType)) {
        LOG.error("Incompatible target type {}", iname);
        return null;
      }
      return relation;
    }
  }

}
