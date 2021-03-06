package nl.knaw.huygens.timbuctoo.crud;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Optional;

public interface ChangeListener {
  void onCreate(Vertex vertex);

  void onUpdate(Optional<Vertex> oldVertex, Vertex newVertex);
}
