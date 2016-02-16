package nl.knaw.huygens.timbuctoo.search.description;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import nl.knaw.huygens.timbuctoo.search.SearchResult;
import nl.knaw.huygens.timbuctoo.server.rest.search.SearchRequestV2_1;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static nl.knaw.huygens.timbuctoo.search.VertexMatcher.likeVertex;
import static nl.knaw.huygens.timbuctoo.util.TestGraphBuilder.newGraph;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AbstractSearchDescriptionTest {

  @Test
  public void executeCreatesASearchResult() {
    AbstractSearchDescription instance = searchDescription().build();

    SearchResult searchResult = instance.execute(newGraph().build(), new SearchRequestV2_1());

    assertThat(searchResult, is(Matchers.notNullValue()));
  }

  @Test
  public void executeCreatesASearchResultWithTheSortableAndFullTextSearchFieldsOfTheDescription() {
    String sortableField1 = "sortableField1";
    String sortableField2 = "sortableField2";
    String searchField1 = "searchField1";
    String searchField2 = "searchField2";
    AbstractSearchDescription instance = searchDescription()
      .withSortableFields(sortableField1, sortableField2)
      .withFullTextSearchFields(searchField1, searchField2)
      .build();

    SearchResult searchResult = instance.execute(newGraph().build(), new SearchRequestV2_1());

    assertThat(searchResult.getSortableFields(), containsInAnyOrder(sortableField1, sortableField2));
    assertThat(searchResult.getFullTextSearchFields(), containsInAnyOrder(searchField1, searchField2));
  }

  // TODO extract ref creator class
  @Test
  public void executeCreatesARefForEachLatestVertexWithTheRightType() {
    PropertyDescriptor idDescriptor = mock(PropertyDescriptor.class);
    PropertyDescriptor displayNameDescriptor = mock(PropertyDescriptor.class);
    PropertyDescriptor dataDescriptor1 = mock(PropertyDescriptor.class);
    PropertyDescriptor dataDescriptor2 = mock(PropertyDescriptor.class);
    String type = "type";
    AbstractSearchDescription instance = searchDescription()
      .withType(type)
      .withIdDescriptor(idDescriptor)
      .withDisplayNameDescriptor(displayNameDescriptor)
      .withDataDescriptor("desc1", dataDescriptor1)
      .withDataDescriptor("desc2", dataDescriptor2)
      .build();
    Graph graph = newGraph()
      .withVertex(vertex -> vertex.withTimId("id").isLatest(true).withType(type))
      .withVertex(vertex -> vertex.withTimId("id1").isLatest(true).withType("otherType"))
      .withVertex(vertex -> vertex.withTimId("id").isLatest(false).withType(type))
      .build();

    SearchResult searchResult = instance.execute(graph, new SearchRequestV2_1());

    assertThat(searchResult.getRefs(), is(not(empty())));

    verify(idDescriptor, times(1)).get(argThat(likeVertex().withType(type)));
    verify(displayNameDescriptor, times(1)).get(argThat(likeVertex().withType(type)));
    verify(dataDescriptor1, times(1)).get(argThat(likeVertex().withType(type)));
    verify(dataDescriptor2, times(1)).get(argThat(likeVertex().withType(type)));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void executeLetsEachFacetDescriptionFillAListOfFacets() {
    FacetDescription facetDescription1 = mock(FacetDescription.class);
    FacetDescription facetDescription2 = mock(FacetDescription.class);
    String type = "type";
    AbstractSearchDescription instance = searchDescription()
      .withType(type)
      .withFacetDescription(facetDescription1)
      .withFacetDescription(facetDescription2)
      .build();
    Graph graph = newGraph()
      .withVertex(vertex -> vertex.withTimId("id").isLatest(true).withType(type))
      .withVertex(vertex -> vertex.withTimId("id1").isLatest(true).withType("otherType"))
      .withVertex(vertex -> vertex.withTimId("id").isLatest(false).withType(type))
      .build();

    SearchResult searchResult = instance.execute(graph, new SearchRequestV2_1());

    assertThat(searchResult.getFacets(), is(Matchers.notNullValue()));

    ArgumentCaptor<GraphTraversal> captor = ArgumentCaptor.forClass(GraphTraversal.class);
    verify(facetDescription1, times(1)).getFacet(captor.capture());
    assertThat(((GraphTraversal<Vertex, Vertex>) captor.getValue()).toList(), contains(likeVertex().withType(type)));
    ArgumentCaptor<GraphTraversal> captor1 = ArgumentCaptor.forClass(GraphTraversal.class);
    verify(facetDescription2, times(1)).getFacet(captor1.capture());
    assertThat(((GraphTraversal<Vertex, Vertex>) captor1.getValue()).toList(), contains(likeVertex().withType(type)));
  }

  @Test
  public void executeLetsEachFacetDescriptorFilterTheSearchResult() {
    FacetDescription facetDescription1 = mock(FacetDescription.class);
    FacetDescription facetDescription2 = mock(FacetDescription.class);
    AbstractSearchDescription instance = searchDescription()
      .withFacetDescription(facetDescription1)
      .withFacetDescription(facetDescription2)
      .build();
    Graph graph = newGraph().build();
    SearchRequestV2_1 searchRequest = new SearchRequestV2_1();

    instance.execute(graph, searchRequest);

    verify(facetDescription1).filter(any(GraphTraversal.class), argThat(is(searchRequest.getFacetValues())));
    verify(facetDescription2).filter(any(GraphTraversal.class), argThat(is(searchRequest.getFacetValues())));
  }

  // TODO add tests to make sure the filtering happens before the creation of the facets and the results.

  private AbstractSearchDescriptionBuilder searchDescription() {
    return new AbstractSearchDescriptionBuilder();
  }

  static class DefaultSearchDescription extends AbstractSearchDescription {
    private final List<String> sortableFields;

    private final List<String> fullTextSearchFields;
    private final String type;
    private final PropertyDescriptor idDescriptor;
    private final List<FacetDescription> facetDescriptions;
    private final Map<String, PropertyDescriptor> dataPropertyDescriptors;
    private final PropertyDescriptor displayNameDescriptor;

    public DefaultSearchDescription(PropertyDescriptor idDescriptor, PropertyDescriptor displayNameDescriptor,
                                    List<FacetDescription> facetDescriptions,
                                    Map<String, PropertyDescriptor> dataPropertyDescriptors,
                                    List<String> sortableFields, List<String> fullTextSearchFields,
                                    String type) {
      this.facetDescriptions = facetDescriptions;
      this.dataPropertyDescriptors = dataPropertyDescriptors;
      this.displayNameDescriptor = displayNameDescriptor;
      this.idDescriptor = idDescriptor;
      this.sortableFields = sortableFields;
      this.fullTextSearchFields = fullTextSearchFields;
      this.type = type;
    }

    @Override
    public List<String> getSortableFields() {
      return sortableFields;
    }

    @Override
    public List<String> getFullTextSearchFields() {
      return fullTextSearchFields;
    }

    @Override
    protected List<FacetDescription> getFacetDescriptions() {
      return facetDescriptions;
    }

    @Override
    protected Map<String, PropertyDescriptor> getDataPropertyDescriptors() {
      return dataPropertyDescriptors;
    }

    @Override
    protected PropertyDescriptor getDisplayNameDescriptor() {
      return displayNameDescriptor;
    }

    @Override
    protected PropertyDescriptor getIdDescriptor() {
      return idDescriptor;
    }

    @Override
    protected String getType() {
      return type;
    }

  }

  private static class AbstractSearchDescriptionBuilder {

    private PropertyDescriptor idDescriptor;
    private PropertyDescriptor displayNameDescriptor;
    private List<FacetDescription> facetDescriptions;
    private Map<String, PropertyDescriptor> dataPropertyDescriptions;
    private List<String> sortableFields;
    private List<String> fullTextSearchFields;
    private String type;

    private AbstractSearchDescriptionBuilder() {
      idDescriptor = vertex -> "";
      displayNameDescriptor = vertex -> "";
      facetDescriptions = Lists.newArrayList();
      dataPropertyDescriptions = Maps.newHashMap();
      sortableFields = Lists.newArrayList();
      fullTextSearchFields = Lists.newArrayList();
      type = null;
    }

    public AbstractSearchDescription build() {
      return new DefaultSearchDescription(idDescriptor, displayNameDescriptor, facetDescriptions,
        dataPropertyDescriptions, sortableFields, fullTextSearchFields, type);
    }

    public AbstractSearchDescriptionBuilder withFacetDescription(FacetDescription facetDescription) {
      facetDescriptions.add(facetDescription);
      return this;
    }

    public AbstractSearchDescriptionBuilder withIdDescriptor(PropertyDescriptor idDescriptor) {
      this.idDescriptor = idDescriptor;
      return this;
    }

    public AbstractSearchDescriptionBuilder withDisplayNameDescriptor(PropertyDescriptor displayNameDescriptor) {
      this.displayNameDescriptor = displayNameDescriptor;
      return this;
    }

    private AbstractSearchDescriptionBuilder withDataDescriptor(String propertyName,
                                                                PropertyDescriptor dataDescriptor) {
      this.dataPropertyDescriptions.put(propertyName, dataDescriptor);
      return this;
    }

    private AbstractSearchDescriptionBuilder withType(String type) {
      this.type = type;

      return this;
    }

    private AbstractSearchDescriptionBuilder withSortableFields(String... sortableFields) {
      this.sortableFields.addAll(Arrays.asList(sortableFields));
      return this;
    }

    private AbstractSearchDescriptionBuilder withFullTextSearchFields(String... fullTextSearchFields) {
      this.fullTextSearchFields.addAll(Arrays.asList(fullTextSearchFields));
      return this;
    }
  }
}
