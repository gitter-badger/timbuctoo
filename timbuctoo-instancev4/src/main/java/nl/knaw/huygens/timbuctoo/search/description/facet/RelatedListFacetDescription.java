package nl.knaw.huygens.timbuctoo.search.description.facet;

import nl.knaw.huygens.timbuctoo.search.description.FacetDescription;
import nl.knaw.huygens.timbuctoo.search.description.PropertyParser;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * A facet description that creates a "LIST" facet with properties from connected vertices.
 */
public class RelatedListFacetDescription implements FacetDescription {
  private final String facetName;
  private final String propertyName;
  private final PropertyParser parser;
  private final String[] relations;

  public RelatedListFacetDescription(String facetName, String propertyName, PropertyParser parser,
                                     String... relations) {
    this.facetName = facetName;
    this.propertyName = propertyName;
    this.parser = parser;
    this.relations = relations;
  }

  @Override
  public Facet getFacet(GraphTraversal<Vertex, Vertex> searchResult) {
    Map<String, Long> counts = searchResult.to(Direction.OUT, relations).has(propertyName)
      .<String>groupCount().by(propertyName).next();

    List<Facet.Option> options = counts.entrySet().stream().map(
      count -> new Facet.DefaultOption(parser.parse(count.getKey()), count.getValue())).collect(toList());

    return new Facet(facetName, options, "LIST");
  }
}
