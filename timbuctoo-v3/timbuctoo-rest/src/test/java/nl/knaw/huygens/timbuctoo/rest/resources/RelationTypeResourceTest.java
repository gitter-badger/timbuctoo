package nl.knaw.huygens.timbuctoo.rest.resources;

/*
 * #%L
 * Timbuctoo REST api
 * =======
 * Copyright (C) 2012 - 2015 Huygens ING
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import nl.knaw.huygens.timbuctoo.Repository;
import nl.knaw.huygens.timbuctoo.config.TypeRegistry;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.ModelException;
import nl.knaw.huygens.timbuctoo.model.RelationType;
import nl.knaw.huygens.timbuctoo.model.util.RelationTypeBuilder;
import nl.knaw.huygens.timbuctoo.rest.TimbuctooException;
import nl.knaw.huygens.timbuctoo.storage.StorageIterator;
import nl.knaw.huygens.timbuctoo.storage.StorageIteratorStub;
import nl.knaw.huygens.timbuctoo.storage.ValidationException;
import nl.knaw.huygens.timbuctoo.vre.VRECollection;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.model.AnotherPrimitiveDomainEntity;
import test.model.BaseDomainEntity;
import test.model.PrimitiveDomainEntity;

public class RelationTypeResourceTest {

  private static TypeRegistry registry;

  @BeforeClass
  public static void setupRegistry() throws ModelException {
    registry = TypeRegistry.getInstance().init("test.model.*");
  }

  @AfterClass
  public static void clearRegistry() {
    registry = null;
  }

  // ---------------------------------------------------------------------------

  private Repository repository;
  private RelationTypeResource resource;
  private VRECollection vreCollection;

  @Before
  public void setup() {
    vreCollection = mock(VRECollection.class);
    repository = mock(Repository.class);
    when(repository.getTypeRegistry()).thenReturn(registry);
    resource = new RelationTypeResource(registry, repository, vreCollection);
  }

  @Test
  public void testGetAvailableRelationTypesWithNoName() {
    StorageIterator<RelationType> iterator = StorageIteratorStub.newInstance(mock(RelationType.class), mock(RelationType.class));
    when(repository.getSystemEntities(RelationType.class)).thenReturn(iterator);

    List<RelationType> result = resource.getAvailableRelationTypes(null);

    Assert.assertEquals(2, result.size());
  }

  @Test(expected = TimbuctooException.class)
  public void testGetAvailableRelationTypesWithInvalidName() {
    resource.getAvailableRelationTypes("invalid");
  }

  @Test
  public void testGetAvailableRelationTypesForPrimitive() throws ValidationException {
    getAvailableRelationTypesWithName("basedomainentity");
  }

  @Test
  public void testGetAvailableRelationTypesForProject() throws ValidationException {
    getAvailableRelationTypesWithName("subadomainentity");
  }

  private void getAvailableRelationTypesWithName(String iname) throws ValidationException {
    RelationType type1 = createRelationType(BaseDomainEntity.class, PrimitiveDomainEntity.class);
    RelationType type2 = createRelationType(BaseDomainEntity.class, AnotherPrimitiveDomainEntity.class);
    RelationType type3 = createRelationType(PrimitiveDomainEntity.class, AnotherPrimitiveDomainEntity.class);
    StorageIterator<RelationType> iterator = StorageIteratorStub.newInstance(type1, type2, type3);
    when(repository.getSystemEntities(RelationType.class)).thenReturn(iterator);

    List<RelationType> result = resource.getAvailableRelationTypes(iname);

    Assert.assertTrue(result.contains(type1));
    Assert.assertTrue(result.contains(type2));
    Assert.assertFalse(result.contains(type3));
  }

  private RelationType createRelationType(Class<? extends DomainEntity> sourceType, Class<? extends DomainEntity> targetType) throws ValidationException {
    RelationType entity = RelationTypeBuilder.newInstance().withSourceType(sourceType).withTargetType(targetType).build();
    entity.validateForAdd(repository);
    return entity;
  }

}
