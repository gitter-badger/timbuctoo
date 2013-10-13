package nl.knaw.huygens.timbuctoo.storage.mongo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import nl.knaw.huygens.timbuctoo.storage.mongo.MongoDiffTest.Foo;

import org.junit.Test;
import org.mockito.Mockito;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;

public class MongoUtilsTest {

  @Test
  public void testGetCollectionName() {
    assertEquals("foo", MongoUtils.getCollectionName(Foo.class));
  }

  @Test
  public void testGetVersioningCollectionName() {
    assertEquals("foo_versions", MongoUtils.getVersioningCollectionName(Foo.class));
  }

  @Test
  public void testGetObjectForDoc() {
    Foo x = new Foo();
    x.name = "blub";
    try {
      assertEquals(new BasicDBObject("name", "blub"), MongoUtils.getObjectForDoc(x));
    } catch (IOException e) {
      fail();
    }
  }

  @Test
  public void testGetCollection() {
    DB db = Mockito.mock(DB.class);
    DBCollection col = Mockito.mock(DBCollection.class);
    when(db.getCollection("foo")).thenReturn(col);
    MongoUtils.getCollection(db, Foo.class);
    verify(db).getCollection("foo");
  }

  @Test
  public void testGetVersioningCollection() {
    DB db = Mockito.mock(DB.class);
    DBCollection col = Mockito.mock(DBCollection.class);
    when(db.getCollection("foo_versions")).thenReturn(col);
    MongoUtils.getVersioningCollection(db, Foo.class);
    verify(db).getCollection("foo_versions");
  }

}
