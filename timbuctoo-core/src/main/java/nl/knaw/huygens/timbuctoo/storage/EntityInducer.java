package nl.knaw.huygens.timbuctoo.storage;

/*
 * #%L
 * Timbuctoo core
 * =======
 * Copyright (C) 2012 - 2014 Huygens ING
 * =======
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Set;

import nl.knaw.huygens.timbuctoo.config.BusinessRules;
import nl.knaw.huygens.timbuctoo.config.TypeRegistry;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.model.Role;
import nl.knaw.huygens.timbuctoo.model.SystemEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

public class EntityInducer {

  private static final Logger LOG = LoggerFactory.getLogger(EntityInducer.class);

  private final ObjectMapper jsonMapper;

  @Inject
  public EntityInducer() {
    jsonMapper = new ObjectMapper();
  }

  /**
   * Converts a system entity to a Json tree.
   */
  public <T extends SystemEntity> JsonNode convertSystemEntityForAdd(Class<T> type, T entity) {
    checkArgument(BusinessRules.allowSystemEntityAdd(type));

    FieldMap fieldMap = new FieldMap(type, Entity.class);
    return createJsonTree(entity, fieldMap);
  }

  /**
   * Converts a system entity to a Json tree and combines it with an existing Json tree.
   */
  public <T extends SystemEntity> JsonNode convertSystemEntityForUpdate(Class<T> type, T entity, ObjectNode tree) {
    FieldMap fieldMap = new FieldMap(type);
    return updateJsonTree(tree, entity, fieldMap);
  }

  /**
   * Converts a domain entity to a Json tree.
   */
  public <T extends DomainEntity> JsonNode convertDomainEntityForAdd(Class<T> type, T entity) {
    checkArgument(BusinessRules.allowDomainEntityAdd(type));

    FieldMap fieldMap = new FieldMap(type, Entity.class);
    ObjectNode tree = createJsonTree(entity, fieldMap);

    fieldMap = new FieldMap(type.getSuperclass());
    tree = updateJsonTree(tree, entity, fieldMap);

    for (Role role : entity.getRoles()) {
      Class<? extends Role> roleType = role.getClass();
      if (BusinessRules.allowRoleAdd(roleType)) {
        fieldMap = new FieldMap(roleType, Role.class);
        tree = updateJsonTree(tree, role, fieldMap);
        fieldMap = new FieldMap(roleType.getSuperclass());
        tree = updateJsonTree(tree, role, fieldMap);
      } else {
        LOG.error("Not allowed to add {}", roleType);
        throw new IllegalStateException("Not allowed to add role");
      }
    }

    return tree;
  }

  /**
   * Converts a domain entity to a Json tree and combines it with an existing Json tree.
   * Note that this method only handles the variant that correspondeds with {@code type},
   * either a primitive domain entity or a project domain entity.
   */
  public <T extends DomainEntity> JsonNode convertDomainEntityForUpdate(Class<T> type, T entity, ObjectNode tree) {
    Class<?> stopType = TypeRegistry.toBaseDomainEntity(type);
    FieldMap fieldMap = new FieldMap(type, stopType);
    if (type == stopType) {
      tree = updateJsonTree(tree, entity, fieldMap);
    } else {
      tree = updateJsonTree(tree, entity, fieldMap.removeSharedFields());
    }

    for (Role role : entity.getRoles()) {
      Class<?> roleType = role.getClass();
      Class<?> baseType = (roleType.getSuperclass() == Role.class) ? roleType : roleType.getSuperclass();
      fieldMap = new FieldMap(roleType, baseType);
      tree = updateJsonTree(tree, role, fieldMap);
    }

    return tree;
  }

  public JsonNode adminSystemEntity(SystemEntity entity, ObjectNode tree) {
    FieldMap fieldMap = new FieldMap(SystemEntity.class, Entity.class);
    return updateJsonTree(tree, entity, fieldMap);
  }

  public JsonNode adminDomainEntity(DomainEntity entity, ObjectNode tree) {
    FieldMap fieldMap = new FieldMap(DomainEntity.class, Entity.class);
    return updateJsonTree(tree, entity, fieldMap);
  }

  // -------------------------------------------------------------------

  /**
   * Updates a Json tree given an object and a field map.
   */
  private ObjectNode updateJsonTree(ObjectNode tree, Object object, FieldMap fieldMap) {
    ObjectNode newTree = createJsonTree(object, fieldMap);
    return merge(tree, newTree, fieldMap.keySet());
  }

  /**
   * Creates a Json tree given an object and a field map.
   */
  private ObjectNode createJsonTree(Object object, FieldMap fieldMap) {
    PropertyMap properties = new PropertyMap(object, fieldMap);
    return jsonMapper.valueToTree(properties);
  }

  /**
   * Merges into a tree the values corresponding to the specified keys of the new tree.
   */
  private ObjectNode merge(ObjectNode tree, ObjectNode newTree, Set<String> keys) {
    for (String key : keys) {
      JsonNode newValue = newTree.get(key);
      if (newValue != null) {
        tree.put(key, newValue);
      } else {
        tree.remove(key);
      }
    }
    return tree;
  }

}
