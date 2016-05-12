package nl.knaw.huygens.timbuctoo.search.description.indexes;

import com.google.common.collect.Lists;
import nl.knaw.huygens.timbuctoo.model.Change;
import nl.knaw.huygens.timbuctoo.model.Datable;
import nl.knaw.huygens.timbuctoo.model.PersonNames;
import nl.knaw.huygens.timbuctoo.search.description.IndexDescription;
import nl.knaw.huygens.timbuctoo.search.description.PropertyDescriptor;
import nl.knaw.huygens.timbuctoo.search.description.PropertyParser;
import nl.knaw.huygens.timbuctoo.search.description.property.PropertyDescriptorFactory;
import nl.knaw.huygens.timbuctoo.search.description.property.WwDocumentDisplayNameDescriptor;
import nl.knaw.huygens.timbuctoo.search.description.propertyparser.PropertyParserFactory;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class WwDocumentIndexDescription implements IndexDescription {

  private static class WwDocumentSortFieldDescription implements IndexerSortFieldDescription {
    private static final String PREFIX = "wwdocument";
    private static final String POSTFIX = "sort";
    private final PropertyParser parser;
    private final Comparable<?> defaultValue;
    private final Class<?> type;
    private final String name;

    public WwDocumentSortFieldDescription(String name, Comparable<?> defaultValue, PropertyParser parser,
                                          Class<?> type) {
      this.parser = parser;
      this.defaultValue = defaultValue;
      this.type = type;
      this.name = name;
    }

    @Override
    public String getSortPropertyName() {
      // FIXME: string concatenating methods like this should be delegated to a configuration class
      if (name.equals("modified")) {
        return "modified_sort";
      }
      return String.format("%s_%s_%s", PREFIX, name, POSTFIX);
    }

    @Override
    public String getPropertyName() {
      // FIXME: string concatenating methods like this should be delegated to a configuration class
      if (name.equals("modified")) {
        return name;
      }
      return String.format("%s_%s", PREFIX, name);
    }

    @Override
    public PropertyParser getParser() {
      return parser;
    }

    @Override
    public Comparable<?> getDefaultValue() {
      return defaultValue;
    }

    @Override
    public Class<?> getType() {
      return type;
    }
  }



  private final List<IndexerSortFieldDescription> sortFieldDescriptions;

  private final PropertyDescriptor displayNameDescriptor;

  public WwDocumentIndexDescription() {
    final PropertyParserFactory propertyParserFactory = new PropertyParserFactory();
    this.sortFieldDescriptions = Lists.newArrayList(
            new WwDocumentSortFieldDescription(
                    "modified", 0L, propertyParserFactory.getParser(Change.class), Long.class),
            new WwDocumentSortFieldDescription(
                    "creator", "", propertyParserFactory.getParser(PersonNames.class), String.class)
    );

    displayNameDescriptor = new WwDocumentDisplayNameDescriptor();
  }

  @Override
  public List<IndexerSortFieldDescription> getSortFieldDescriptions() {
    return sortFieldDescriptions;
  }

  @Override
  public void addIndexedSortProperties(Vertex vertex) {
    for (IndexerSortFieldDescription description : sortFieldDescriptions) {
      final PropertyParser parser = description.getParser();
      final String propertyName = description.getPropertyName();
      final String sortPropertyName = description.getSortPropertyName();

      if (propertyName.equals("modified")) {
        Comparable<?> parsed = vertex.property(propertyName).isPresent() ?
                parser.parseForSort((String) vertex.property(propertyName).value()) :
                parser.parseForSort(null);

        if (parsed == null) {
          vertex.property(sortPropertyName, description.getDefaultValue());
        } else {
          vertex.property(sortPropertyName, parsed);
        }
      } else if (sortPropertyName.equals("wwdocument_creator_sort")) {
        TreeSet<String> creators = new TreeSet<>();
        vertex.vertices(Direction.OUT, "isCreatedBy").forEachRemaining(outVertex -> {
          if (outVertex.property("wwperson_names_sort").isPresent()) {
            creators.add((String) outVertex.property("wwperson_names_sort").value());
          }
        });

        if (creators.size() > 0) {
          vertex.property(sortPropertyName, creators.first());
        } else {
          vertex.property(sortPropertyName, description.getDefaultValue());
        }
      }
    }
  }

  @Override
  public void addToFulltextIndex(Vertex vertex, GraphDatabaseService graphDatabase) {
    final IndexManager indexManager = graphDatabase.index();
    final Map<String, String> indexConfig = MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext");
    final Index<Node> index = indexManager.forNodes("wwdocuments", indexConfig);
    final String displayName = displayNameDescriptor.get(vertex);

    long id = (long) vertex.id();
    Node neo4jNode = graphDatabase.getNodeById(id);
    index.add(neo4jNode, "displayName", displayName == null ? "" : displayName);
    index.add(neo4jNode, "tim_id", vertex.property("tim_id").value());
  }
}
