package nl.knaw.huygens.repository.search.model;

import nl.knaw.huygens.repository.facet.IndexAnnotation;
import nl.knaw.huygens.repository.model.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SimpleClassWithOneFullTextSearchField extends Entity {

  private String simpleIndexField;

  @Override
  @JsonIgnore
  @IndexAnnotation(fieldName = "desc")
  public String getDisplayName() {
    // TODO Auto-generated method stub
    return null;
  }

  @IndexAnnotation(fieldName = "dynamic_t_simple", isFaceted = true, title = "Simple")
  public String getSimpleIndexField() {
    return simpleIndexField;
  }

  public void setSimpleIndexField(String simpleIndexField) {
    this.simpleIndexField = simpleIndexField;
  }

}
