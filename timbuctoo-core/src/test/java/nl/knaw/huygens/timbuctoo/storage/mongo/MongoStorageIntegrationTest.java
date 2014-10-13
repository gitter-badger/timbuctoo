package nl.knaw.huygens.timbuctoo.storage.mongo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.net.UnknownHostException;

import nl.knaw.huygens.timbuctoo.config.TypeRegistry;
import nl.knaw.huygens.timbuctoo.model.ModelException;
import nl.knaw.huygens.timbuctoo.model.util.Change;
import nl.knaw.huygens.timbuctoo.storage.EntityInducer;
import nl.knaw.huygens.timbuctoo.storage.EntityReducer;
import nl.knaw.huygens.timbuctoo.storage.StorageException;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import test.variation.model.projecta.ProjectADomainEntity;

import com.mongodb.MongoClient;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

public class MongoStorageIntegrationTest {
  private static final Class<ProjectADomainEntity> DOMAIN_TYPE = ProjectADomainEntity.class;
  private static final Change DEFAULT_CHANGE = new Change();
  private static final MongodStarter starter = MongodStarter.getDefaultInstance();
  private MongodExecutable mongodExe;
  private MongodProcess mongod;
  private MongoClient mongo;
  private MongoStorage instance;

  @Before
  public void setUp() throws UnknownHostException, IOException, ModelException {
    mongodExe = starter.prepare(new MongodConfigBuilder().version(Version.Main.PRODUCTION)//
        .net(new Net(12345, Network.localhostIsIPv6()))//
        .build());

    mongod = mongodExe.start();
    mongo = new MongoClient("localhost", 12345);

    MongoDB mongoDB = new MongoDB(mongo, mongo.getDB("test"));

    TypeRegistry registry = TypeRegistry.getInstance();
    instance = new MongoStorage(mongoDB, new EntityIds(registry, mongoDB), new EntityInducer(), new EntityReducer(registry));
  }

  @After
  public void tearDown() {
    mongod.stop();
    mongodExe.stop();
  }

  @Test
  public void writeDomainEntityShouldAddAnItemToTheDatabase() throws StorageException {
    ProjectADomainEntity entity = createEntityWithGeneralTestDocValue("test");

    // action
    String id = instance.addDomainEntity(DOMAIN_TYPE, entity, DEFAULT_CHANGE);

    // verify
    ProjectADomainEntity foundItem = instance.getItem(DOMAIN_TYPE, id);
    assertThat(foundItem, isNotNullProjectADomainEntity());
    assertThat(foundItem.generalTestDocValue, is(equalTo(entity.generalTestDocValue)));
  }

  @Test
  public void setPIDFillsThePIDpropertyAndAddsTheVersionToVersionCollection() throws StorageException {
    // setup
    ProjectADomainEntity entity = createEntityWithGeneralTestDocValue("test");
    String id = instance.addDomainEntity(DOMAIN_TYPE, entity, DEFAULT_CHANGE);

    // action
    String pid = "randomPID";
    instance.setPID(DOMAIN_TYPE, id, pid);

    // verify
    ProjectADomainEntity foundItem = instance.getItem(DOMAIN_TYPE, id);
    assertThat(foundItem, isNotNullProjectADomainEntity());
    assertThat(foundItem.getPid(), is(notNullValue(String.class)));

    assertThat(instance.getRevision(ProjectADomainEntity.class, id, 1), isNotNullProjectADomainEntity());
  }

  private Matcher<ProjectADomainEntity> isNotNullProjectADomainEntity() {
    return is(notNullValue(ProjectADomainEntity.class));
  }

  private ProjectADomainEntity createEntityWithGeneralTestDocValue(String generalTestDocValue) {
    ProjectADomainEntity entity = new ProjectADomainEntity();
    entity.generalTestDocValue = generalTestDocValue;
    return entity;
  }
}
