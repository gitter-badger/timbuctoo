package nl.knaw.huygens.timbuctoo.storage.graph.tinkerpop;

import nl.knaw.huygens.timbuctoo.config.TypeRegistry;
import nl.knaw.huygens.timbuctoo.model.ModelException;
import nl.knaw.huygens.timbuctoo.storage.DBIntegrationTestHelper;
import nl.knaw.huygens.timbuctoo.storage.Storage;
import nl.knaw.huygens.timbuctoo.storage.graph.GraphLegacyStorageWrapper;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class TinkerpopDBIntegrationTestHelper implements DBIntegrationTestHelper {

  private Graph graph;

  @Override
  public void startCleanDB() throws Exception {
    graph = new TinkerGraph();
  }

  @Override
  public void stopDB() {
    graph.shutdown();
  }

  @Override
  public Storage createStorage(TypeRegistry typeRegistry) throws ModelException {
    return new GraphLegacyStorageWrapper(new TinkerpopStorage(graph));
  }

}
