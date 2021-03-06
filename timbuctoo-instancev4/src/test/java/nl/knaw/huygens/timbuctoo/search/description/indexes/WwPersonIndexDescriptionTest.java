package nl.knaw.huygens.timbuctoo.search.description.indexes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.huygens.timbuctoo.crud.changelistener.DenormalizedSortFieldUpdater;
import nl.knaw.huygens.timbuctoo.crud.TinkerpopJsonCrudService;
import nl.knaw.huygens.timbuctoo.model.Change;
import nl.knaw.huygens.timbuctoo.model.PersonName;
import nl.knaw.huygens.timbuctoo.model.PersonNameComponent;
import nl.knaw.huygens.timbuctoo.util.JsonBuilder;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static nl.knaw.huygens.timbuctoo.crud.JsonCrudServiceBuilder.newJsonCrudService;
import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsn;
import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsnO;
import static nl.knaw.huygens.timbuctoo.util.TestGraphBuilder.newGraph;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

public class WwPersonIndexDescriptionTest {

  @Test
  public void getSortIndexPropertyNamesReturnsPropertyNamesForAllTypesAndFields() {
    WwPersonIndexDescription instance = new WwPersonIndexDescription();

    Set<String> results = instance.getSortFieldDescriptions().stream()
            .map(IndexerSortFieldDescription::getSortPropertyName)
            .collect(Collectors.toSet());

    assertThat(results, containsInAnyOrder(
            "wwperson_names_sort",
            "wwperson_deathDate_sort",
            "wwperson_birthDate_sort",
            "modified_sort"
    ));
  }

  @Test
  public void addIndexedSortPropertiesSetsTheSortIndexProperties() throws JsonProcessingException {
    long timeStampOnJan20th2016 = 1453290593000L;
    Graph graph = newGraph()
            .withVertex(v -> v
                    .withVre("ww")
                    .withType("person")
                    .withProperty("wwperson_names", getPersonName("testfore", "testsur2"))
                    .withProperty("wwperson_deathDate", "\"2015-05-01\"")
                    .withProperty("wwperson_birthDate", "\"2010-05-01\"")
                    .withProperty("modified", getChange(timeStampOnJan20th2016))
            )
            .build();
    WwPersonIndexDescription instance = new WwPersonIndexDescription();
    Vertex vertex = graph.traversal().V().toList().get(0);

    instance.addIndexedSortProperties(vertex);

    assertThat(vertex.property("wwperson_names_sort").value(), equalTo("testsur2, testfore"));
    assertThat(vertex.property("wwperson_birthDate_sort").value(), equalTo(2010));
    assertThat(vertex.property("wwperson_deathDate_sort").value(), equalTo(2015));
    assertThat(vertex.property("modified_sort").value(), equalTo(timeStampOnJan20th2016));

  }

  @Test
  public void addIndexedSortPropertiesSetsTheSortIndexPropertyToEmptyStringWhenPropertyIsMissing() {
    Graph graph = newGraph()
            .withVertex(v -> v
                    .withVre("ww")
                    .withType("person"))
            .build();
    WwPersonIndexDescription instance = new WwPersonIndexDescription();
    Vertex vertex = graph.traversal().V().toList().get(0);

    instance.addIndexedSortProperties(vertex);

    assertThat(vertex.property("wwperson_names_sort").value(), equalTo(""));
    assertThat(vertex.property("wwperson_birthDate_sort").value(), equalTo(0));
    assertThat(vertex.property("wwperson_deathDate_sort").value(), equalTo(0));
    assertThat(vertex.property("modified_sort").value(), equalTo(0L));

  }

  @Test
  public void invokesIndexDescriptionAddIndexedSortPropertiesForWwPersonsOnUpdate() throws Exception {
    String id = UUID.randomUUID().toString();
    Graph graph = newGraph()
            .withVertex(v -> v
                    .withTimId(id)
                    .withProperty("types", "[\"person\", \"wwperson\"]")
                    .withProperty("isLatest", true)
                    .withProperty("rev", 1)
                    .withIncomingRelation("VERSION_OF", "orig")
            )
            .withVertex("orig", v -> v
                    .withTimId(id)
                    .withProperty("isLatest", false)
                    .withProperty("rev", 1)
            )
            .build();
    TinkerpopJsonCrudService instance = newJsonCrudService()
      .withChangeListener(new DenormalizedSortFieldUpdater(new IndexDescriptionFactory()))
      .forGraph(graph);

    instance.replace("wwpersons", UUID.fromString(id), jsnO("^rev", jsn(1)), "");


    Vertex vertex = graph.traversal().V().has("tim_id", id).has("isLatest", true).next();

    MatcherAssert.assertThat(vertex.property("wwperson_names_sort").value(), equalTo(""));
    MatcherAssert.assertThat(vertex.property("wwperson_birthDate_sort").value(), equalTo(0));
    MatcherAssert.assertThat(vertex.property("wwperson_deathDate_sort").value(), equalTo(0));
    MatcherAssert.assertThat(vertex.property("modified_sort").value(), Matchers.instanceOf(Long.class));
  }

  @Test
  public void crudServiceInvokesIndexDescriptionAddIndexedSortPropertiesForWwPersonsOnCreate() throws Exception {
    Graph graph = newGraph().build();

    TinkerpopJsonCrudService instance = newJsonCrudService()
      .withChangeListener(new DenormalizedSortFieldUpdater(new IndexDescriptionFactory()))
      .forGraph(graph);

    instance.create("wwpersons", JsonBuilder.jsnO(), "");

    Vertex vertex = graph.vertices().next();

    assertThat(vertex.property("wwperson_names_sort").value(), equalTo(""));
    assertThat(vertex.property("wwperson_birthDate_sort").value(), equalTo(0));
    assertThat(vertex.property("wwperson_deathDate_sort").value(), equalTo(0));
    assertThat(vertex.property("modified_sort").value(), instanceOf(Long.class));
  }

  private String getPersonName(String foreName, String surName) {
    PersonName name = new PersonName();
    name.addNameComponent(PersonNameComponent.Type.FORENAME, foreName);
    name.addNameComponent(PersonNameComponent.Type.SURNAME, surName);
    String nameProp;
    try {
      nameProp = new ObjectMapper().writeValueAsString(name);
    } catch (IOException e) {
      nameProp = "";
    }

    return "{\"list\": [" + nameProp + "]}";
  }


  private String getChange(long timeStamp) {
    Change change = new Change(timeStamp, "user", "vre");
    String changeString;
    try {
      changeString = new ObjectMapper().writeValueAsString(change);
    } catch (JsonProcessingException e) {
      changeString = "";
    }
    return changeString;
  }
}
