package nl.knaw.huygens.timbuctoo.storage.mongo;

import static nl.knaw.huygens.timbuctoo.storage.FieldMapper.propertyName;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.knaw.huygens.timbuctoo.config.TypeRegistry;
import nl.knaw.huygens.timbuctoo.model.DatableSystemEntity;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.model.Role;
import nl.knaw.huygens.timbuctoo.model.TestSystemEntity;
import nl.knaw.huygens.timbuctoo.model.TestSystemEntityPrimitive;
import nl.knaw.huygens.timbuctoo.model.TestSystemEntityPrimitiveCollections;
import nl.knaw.huygens.timbuctoo.model.util.Datable;
import nl.knaw.huygens.timbuctoo.model.util.PersonName;
import nl.knaw.huygens.timbuctoo.model.util.PersonNameComponent.Type;
import nl.knaw.huygens.timbuctoo.variation.model.BaseDomainEntity;
import nl.knaw.huygens.timbuctoo.variation.model.NewTestRole;
import nl.knaw.huygens.timbuctoo.variation.model.TestRole;
import nl.knaw.huygens.timbuctoo.variation.model.projecta.ProjectADomainEntity;
import nl.knaw.huygens.timbuctoo.variation.model.projecta.ProjectANewTestRole;
import nl.knaw.huygens.timbuctoo.variation.model.projecta.ProjectATestDocWithPersonName;
import nl.knaw.huygens.timbuctoo.variation.model.projecta.ProjectATestRole;
import nl.knaw.huygens.timbuctoo.variation.model.projectb.ProjectBDomainEntity;
import nl.knaw.huygens.timbuctoo.variation.model.projectb.ProjectBTestRole;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class VariationReducerTest extends VariationTestBase {

  private static final String GENERAL_TEST_DOC_VALUE = "generalTestDocValue";
  private static final String TEST_ID = "id0000000001";
  private static final String TEST_PID = "test pid";

  private static TypeRegistry registry;

  private VariationReducer reducer;

  @BeforeClass
  public static void setupRegistry() {
    registry = new TypeRegistry("timbuctoo.variation.model timbuctoo.variation.model.projecta timbuctoo.variation.model.projectb");
  }

  @Before
  public void setup() {
    reducer = new VariationReducer(registry);
  }

  @Test
  public void testReduceSystemEntity() throws Exception {
    String name = "test";
    String testValue1 = "testValue1";
    String testValue2 = "testValue2";
    ObjectNode item = createSystemObjectNode(TEST_ID, name, testValue1, testValue2);

    TestSystemEntity expected = new TestSystemEntity();
    expected.setId(TEST_ID);
    expected.setName(name);
    expected.setTestValue1(testValue1);
    expected.setTestValue2(testValue2);

    assertEquals(expected, reducer.reduce(TestSystemEntity.class, item));
  }

  @Test
  public void testReducePrimitiveFields() throws Exception {
    Class<? extends Entity> type = TestSystemEntityPrimitive.class;

    Map<String, Object> map = Maps.newHashMap();
    map.put(propertyName(type, "testBoolean"), "true");
    map.put(propertyName(type, "testChar"), "r");
    map.put(propertyName(type, "testDouble"), "3.14");
    map.put(propertyName(type, "testFloat"), "2.13");
    map.put(propertyName(type, "testInt"), "14");
    map.put(propertyName(type, "testLong"), "15098");
    map.put(propertyName(type, "testShort"), "4");

    ObjectNode node = mapper.valueToTree(map);

    TestSystemEntityPrimitive expected = new TestSystemEntityPrimitive();
    expected.setTestBoolean(true);
    expected.setTestChar('r');
    expected.setTestDouble(3.14);
    expected.setTestFloat(2.13f);
    expected.setTestInt(14);
    expected.setTestLong(15098l);
    expected.setTestShort((short) 4);

    assertEquals(expected, reducer.reduce(type, node));
  }

  @Test
  public void testInduceDatable() throws Exception {
    Class<? extends Entity> type = DatableSystemEntity.class;

    Map<String, Object> map = Maps.newHashMap();
    Datable datable = new Datable("20131011");
    map.put(propertyName(type, "testDatable"), datable.getEDTF());
    map.put("^rev", 0);
    ObjectNode node = mapper.valueToTree(map);

    DatableSystemEntity expected = new DatableSystemEntity();
    expected.setTestDatable(datable);
    expected.setRev(0);

    assertEquals(expected, reducer.reduce(type, node));
  }

  @Test
  public void testReduceSystemEntityPrimitiveCollections() throws Exception {
    Class<? extends Entity> type = TestSystemEntityPrimitiveCollections.class;

    Map<String, Object> map = Maps.newHashMap();
    map.put(propertyName(type, "testStringList"), new String[] { "test", "test1" });
    map.put(propertyName(type, "testIntegerList"), new Integer[] { 1, 13, 42 });

    ObjectNode node = mapper.valueToTree(map);

    TestSystemEntityPrimitiveCollections expected = new TestSystemEntityPrimitiveCollections();
    expected.setTestIntegerList(Lists.newArrayList(1, 13, 42));
    expected.setTestStringList(Lists.newArrayList("test", "test1"));

    assertEquals(expected, reducer.reduce(type, node));
  }

  @Test
  public void testReduceDomainEntityDefault() throws Exception {
    Map<String, Object> map = newBaseDomainEntityMap(TEST_ID, TEST_PID, GENERAL_TEST_DOC_VALUE);
    map.put(propertyName(ProjectADomainEntity.class, "projectAGeneralTestDocValue"), "projectatest");
    ObjectNode node = mapper.valueToTree(map);

    BaseDomainEntity expected = new BaseDomainEntity();
    expected.setId(TEST_ID);
    expected.setPid(TEST_PID);
    expected.generalTestDocValue = GENERAL_TEST_DOC_VALUE;

    assertEquals(expected, reducer.reduce(BaseDomainEntity.class, node));
  }

  @Test
  public void testReduceDomainEntityProjectWithPersonName() throws Exception {
    ProjectATestDocWithPersonName expected = new ProjectATestDocWithPersonName();
    PersonName name = new PersonName();
    name.addNameComponent(Type.FORENAME, "test");
    name.addNameComponent(Type.SURNAME, "test");
    expected.setPersonName(name);

    Map<String, Object> map = Maps.newHashMap();
    map.put(propertyName(ProjectATestDocWithPersonName.class, "personName"), PersonNameMapper.createPersonNameMap(name));
    map.put("^rev", 0);
    map.put(DomainEntity.DELETED, false);
    JsonNode node = mapper.valueToTree(map);

    assertEquals(expected, reducer.reduce(ProjectATestDocWithPersonName.class, node));
  }

  @Test
  public void testReduceDomainEntityProjectSubClass() throws Exception {
    Map<String, Object> map = newBaseDomainEntityMap(TEST_ID, TEST_PID, GENERAL_TEST_DOC_VALUE);
    String projectatestvalue = "projectatest";
    map.put(propertyName(ProjectADomainEntity.class, "projectAGeneralTestDocValue"), projectatestvalue);

    ObjectNode node = mapper.valueToTree(map);

    ProjectADomainEntity expected = new ProjectADomainEntity();
    expected.setId(TEST_ID);
    expected.setPid(TEST_PID);
    expected.generalTestDocValue = GENERAL_TEST_DOC_VALUE;
    expected.projectAGeneralTestDocValue = projectatestvalue;

    assertEquals(expected, reducer.reduce(ProjectADomainEntity.class, node));
  }

  @Test
  public void testReduceDomainEntityProjectSubClassVariation() throws Exception {
    Map<String, Object> map = newBaseDomainEntityMap(TEST_ID, TEST_PID, GENERAL_TEST_DOC_VALUE);
    String projectatestvalue = "projectatest";
    map.put(propertyName(ProjectADomainEntity.class, "projectAGeneralTestDocValue"), projectatestvalue);
    String projectAVariation = "projectAVariation";
    map.put(propertyName(ProjectADomainEntity.class, "generalTestDocValue"), projectAVariation);

    ObjectNode node = mapper.valueToTree(map);

    ProjectADomainEntity expected = new ProjectADomainEntity();
    expected.setId(TEST_ID);
    expected.setPid(TEST_PID);
    expected.generalTestDocValue = projectAVariation;
    expected.projectAGeneralTestDocValue = projectatestvalue;

    assertEquals(expected, reducer.reduce(ProjectADomainEntity.class, node));
  }

  @Test
  public void testReduceDomainEntityWithRole() throws Exception {
    Map<String, Object> map = newBaseDomainEntityMap(TEST_ID, TEST_PID, GENERAL_TEST_DOC_VALUE);
    map.put(propertyName(ProjectBDomainEntity.class, "projectBGeneralTestDocValue"), "testB");
    map.put(propertyName("projectbtestrole", "beeName"), "beeName");
    map.put(propertyName("testrole", "roleName"), "roleName");

    JsonNode node = mapper.valueToTree(map);

    BaseDomainEntity expected = new BaseDomainEntity();
    expected.setId(TEST_ID);
    expected.setPid(TEST_PID);
    expected.generalTestDocValue = GENERAL_TEST_DOC_VALUE;
    TestRole testRole = new TestRole();
    testRole.setRoleName("roleName");
    ArrayList<Role> roles = Lists.newArrayList();
    roles.add(testRole);
    expected.setRoles(roles);

    assertEquals(expected, reducer.reduce(BaseDomainEntity.class, node));
  }

  @Test
  public void testReduceDomainEntityWithMultipleRoles() throws Exception {
    Map<String, Object> map = newBaseDomainEntityMap(TEST_ID, TEST_PID, GENERAL_TEST_DOC_VALUE);
    map.put(propertyName(ProjectADomainEntity.class, "projectAGeneralTestDocValue"), "testB");
    map.put(propertyName("projectatestrole", "projectANewTestRoleName"), "beeName");
    map.put(propertyName("testrole", "roleName"), "roleName");
    map.put(propertyName("newtestrole", "newTestRoleName"), "newTestRoleName");
    map.put(propertyName("projectanewtestrole", "projectANewTestRoleName"), "projectANewTestRoleName");

    JsonNode node = mapper.valueToTree(map);

    BaseDomainEntity expected = new BaseDomainEntity();
    expected.setId(TEST_ID);
    expected.setPid(TEST_PID);
    expected.generalTestDocValue = GENERAL_TEST_DOC_VALUE;
    TestRole testRole = new TestRole();
    testRole.setRoleName("roleName");
    NewTestRole newTestRole = new NewTestRole();
    newTestRole.setNewTestRoleName("newTestRoleName");
    ArrayList<Role> roles = Lists.newArrayList();
    roles.add(testRole);
    roles.add(newTestRole);
    expected.setRoles(roles);

    assertEquals(expected, reducer.reduce(BaseDomainEntity.class, node));
  }

  @Test
  public void testReduceDomainEntityProjectSubClassWithRole() throws Exception {
    Map<String, Object> map = newBaseDomainEntityMap(TEST_ID, TEST_PID, GENERAL_TEST_DOC_VALUE);
    String projectBTestDocValue = "testB";
    map.put(propertyName(ProjectBDomainEntity.class, "projectBGeneralTestDocValue"), projectBTestDocValue);
    map.put(propertyName("projectbtestrole", "beeName"), "beeName");
    map.put(propertyName("testrole", "roleName"), "roleName");

    JsonNode node = mapper.valueToTree(map);

    ProjectBDomainEntity expected = new ProjectBDomainEntity();
    expected.setId(TEST_ID);
    expected.setPid(TEST_PID);
    expected.generalTestDocValue = GENERAL_TEST_DOC_VALUE;
    expected.projectBGeneralTestDocValue = projectBTestDocValue;
    ProjectBTestRole testRole = new ProjectBTestRole();
    testRole.setRoleName("roleName");
    testRole.setBeeName("beeName");

    ArrayList<Role> roles = Lists.newArrayList();
    roles.add(testRole);
    expected.setRoles(roles);

    assertEquals(expected, reducer.reduce(ProjectBDomainEntity.class, node));
  }

  @Test
  public void testReduceDomainEnityProjectSubClassWithRoleNotFilledIn() throws Exception {
    Map<String, Object> map = newBaseDomainEntityMap(TEST_ID, TEST_PID, GENERAL_TEST_DOC_VALUE);
    map.put(propertyName(ProjectBDomainEntity.class, "projectBGeneralTestDocValue"), "testB");
    map.put(propertyName("projectbtestrole", "beeName"), "beeName");
    map.put(propertyName("testrole", "roleName"), "roleName");

    JsonNode node = mapper.valueToTree(map);

    ProjectADomainEntity expected = new ProjectADomainEntity();
    expected.setId(TEST_ID);
    expected.setPid(TEST_PID);
    expected.generalTestDocValue = GENERAL_TEST_DOC_VALUE;
    ProjectATestRole testRole = new ProjectATestRole();
    testRole.setRoleName("roleName");

    ArrayList<Role> roles = Lists.newArrayList();
    roles.add(testRole);
    expected.setRoles(roles);

    assertEquals(expected, reducer.reduce(ProjectADomainEntity.class, node));
  }

  @Test
  public void testReduceDomainEntityProjectSubClassWithMultipleRoles() throws Exception {
    Map<String, Object> map = newBaseDomainEntityMap(TEST_ID, TEST_PID, "test");
    map.put(propertyName(ProjectADomainEntity.class, "projectAGeneralTestDocValue"), "testB");
    map.put(propertyName("projectatestrole", "projectATestRoleName"), "beeName");
    map.put(propertyName("testrole", "roleName"), "roleName");
    map.put(propertyName("newtestrole", "newTestRoleName"), "newTestRoleName");
    map.put(propertyName("projectanewtestrole", "projectANewTestRoleName"), "projectANewTestRoleName");

    JsonNode node = mapper.valueToTree(map);

    ProjectADomainEntity expected = new ProjectADomainEntity();
    expected.setId(TEST_ID);
    expected.setPid(TEST_PID);
    expected.generalTestDocValue = "test";
    expected.projectAGeneralTestDocValue = "testB";
    ProjectATestRole role = new ProjectATestRole();
    role.setProjectATestRoleName("beeName");
    role.setRoleName("roleName");
    ProjectANewTestRole projectANewTestRole = new ProjectANewTestRole();
    projectANewTestRole.setNewTestRoleName("newTestRoleName");
    projectANewTestRole.setProjectANewTestRoleName("projectANewTestRoleName");
    List<Role> roles = Lists.newArrayList();
    roles.add(role);
    roles.add(projectANewTestRole);
    expected.setRoles(roles);

    assertEquals(expected, reducer.reduce(ProjectADomainEntity.class, node));
  }

  @Test
  public void testReduceDomainEntityRequestedVariation() throws Exception {
    Map<String, Object> map = newBaseDomainEntityMap(TEST_ID, TEST_PID, GENERAL_TEST_DOC_VALUE);
    String projectatestvalue = "projectatest";
    map.put(propertyName(ProjectADomainEntity.class, "projectAGeneralTestDocValue"), projectatestvalue);
    String projectAVariation = "projectAVariation";
    map.put(propertyName(ProjectADomainEntity.class, "generalTestDocValue"), projectAVariation);

    ObjectNode node = mapper.valueToTree(map);

    BaseDomainEntity expected = new BaseDomainEntity();
    expected.setId(TEST_ID);
    expected.setPid(TEST_PID);
    expected.generalTestDocValue = projectAVariation;
    String requestedVariation = "projecta";

    assertEquals(expected, reducer.reduce(BaseDomainEntity.class, node, requestedVariation));
  }

  @Test
  public void testReduceDomainEntityWithRoleRequestedVariation() throws Exception {
    Map<String, Object> map = newBaseDomainEntityMap(TEST_ID, TEST_PID, "test");
    map.put(propertyName(ProjectBDomainEntity.class, "projectBGeneralTestDocValue"), "testB");
    map.put(propertyName("projectbtestrole", "beeName"), "beeName");
    map.put(propertyName("testrole", "roleName"), "roleName");
    String projectatestvalue = "projectatest";
    map.put(propertyName(ProjectADomainEntity.class, "projectAGeneralTestDocValue"), projectatestvalue);
    String projectAVariation = "projectAVariation";
    map.put(propertyName(ProjectADomainEntity.class, "generalTestDocValue"), projectAVariation);
    map.put(propertyName("projectatestrole", "projectATestRoleName"), "value");
    map.put(propertyName("projectatestrole", "roleName"), "value");

    JsonNode node = mapper.valueToTree(map);

    BaseDomainEntity expected = new BaseDomainEntity();
    expected.setId(TEST_ID);
    expected.setPid(TEST_PID);
    expected.generalTestDocValue = projectAVariation;
    String requestedVariation = "projecta";
    TestRole testRole = new TestRole();
    testRole.setRoleName("value");
    ArrayList<Role> roles = Lists.newArrayList();
    roles.add(testRole);
    expected.setRoles(roles);

    assertEquals(expected, reducer.reduce(BaseDomainEntity.class, node, requestedVariation));
  }

  //TODO: add tests for reducing revisions / multiple revisions.

}
