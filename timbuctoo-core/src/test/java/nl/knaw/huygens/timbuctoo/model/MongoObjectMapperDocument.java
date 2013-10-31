package nl.knaw.huygens.timbuctoo.model;

import java.util.List;

import nl.knaw.huygens.timbuctoo.annotations.IDPrefix;
import nl.knaw.huygens.timbuctoo.facet.IndexAnnotation;
import nl.knaw.huygens.timbuctoo.model.SystemEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@IDPrefix("TSTD")
public class MongoObjectMapperDocument extends SystemEntity {

  private String name;
  private String testValue1;
  private String testValue2;
  @JsonProperty("propAnnotated")
  private String annotatedProperty;
  private String propWithAnnotatedAccessors;

  private List<String> primitiveTestCollection;
  private List<? extends SystemEntity> nonPrimitiveTestCollection;

  @Override
  @JsonIgnore
  @IndexAnnotation(fieldName = "desc")
  public String getDisplayName() {
    return name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTestValue1() {
    return testValue1;
  }

  public void setTestValue1(String testValue1) {
    this.testValue1 = testValue1;
  }

  public String getTestValue2() {
    return testValue2;
  }

  public void setTestValue2(String testValue2) {
    this.testValue2 = testValue2;
  }

  public String getAnnotatedProperty() {
    return annotatedProperty;
  }

  public void setAnnotatedProperty(String annotatedProperty) {
    this.annotatedProperty = annotatedProperty;
  }

  @JsonProperty("pwaa")
  public String getPropWithAnnotatedAccessors() {
    return propWithAnnotatedAccessors;
  }

  @JsonProperty("pwaa")
  public void setPropWithAnnotatedAccessors(String propWithAnnotatedAccessors) {
    this.propWithAnnotatedAccessors = propWithAnnotatedAccessors;
  }

  public List<String> getPrimitiveTestCollection() {
    return primitiveTestCollection;
  }

  public void setPrimitiveTestCollection(List<String> primitiveTestCollection) {
    this.primitiveTestCollection = primitiveTestCollection;
  }

  public List<? extends SystemEntity> getNonPrimitiveTestCollection() {
    return nonPrimitiveTestCollection;
  }

  public void setNonPrimitiveTestCollection(List<? extends SystemEntity> nonPrimitiveTestCollection) {
    this.nonPrimitiveTestCollection = nonPrimitiveTestCollection;
  }

}