package nl.knaw.huygens.timbuctoo.model.dcar;

/*
 * #%L
 * Timbuctoo model
 * =======
 * Copyright (C) 2012 - 2015 Huygens ING
 * =======
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import static nl.knaw.huygens.timbuctoo.model.dcar.RelTypeNames.HAS_ARCHIVER_KEYWORD;
import static nl.knaw.huygens.timbuctoo.model.dcar.RelTypeNames.HAS_ARCHIVER_PERSON;
import static nl.knaw.huygens.timbuctoo.model.dcar.RelTypeNames.HAS_ARCHIVER_PLACE;
import static nl.knaw.huygens.timbuctoo.model.dcar.RelTypeNames.HAS_SIBLING_ARCHIVER;
import static nl.knaw.huygens.timbuctoo.model.dcar.RelTypeNames.IS_CREATOR_OF;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import nl.knaw.huygens.facetedsearch.model.FacetType;
import nl.knaw.huygens.timbuctoo.facet.IndexAnnotation;
import nl.knaw.huygens.timbuctoo.facet.IndexAnnotations;
import nl.knaw.huygens.timbuctoo.model.Archiver;
import nl.knaw.huygens.timbuctoo.model.RelationRef;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

public class DCARArchiver extends Archiver {

  /** Migration: Name of source file */
  private String origFilename;

  /** ING Forms: "Name" */
  private String nameNld;

  /** ING Forms: "English name"; text searchable */
  public String nameEng;

  /** ING Forms: "Begin date"; date facet */
  private String beginDate;

  /** ING Forms: "End date"; date facet */
  private String endDate;

  /** ING Forms: "Period description" */
  private String periodDescription;

  /** ING Forms: "History/functions/occupations/activities"; text searchable */
  private String history;

  /** ING Forms: "Title(s) related archive(s); as relation " */

  /** ING Forms: "Title(s) related creator(s); as relation " */

  /** ING Forms: "Keyword(s) geography"; as relation ; place facet */

  /** ING Forms: "Keyword(s) subject"; as relation ; subject facet */

  /** ING Forms: "Keyword(s) person"; as relation ; person facet */

  /** ING Forms: "Remarks"; text searchable */
  private String notes;

  /** ING Forms: "Literature" */
  private String literature;

  /** ING Forms: "Record made by-" */
  private String madeBy;

  /** ING Forms: "Reminders" ??? */
  private String reminders;

  /** ING Forms: "Binnenkomende relaties" */
  private XRelated[] related;

  /** ING Forms: ??? ("person", "family") */
  private List<String> types;

  public DCARArchiver() {
    types = Lists.newArrayList();
  }

  @Override
  public String getIdentificationName() {
    return getNameEng();
  }

  public String getOrigFilename() {
    return origFilename;
  }

  public void setOrigFilename(String origFilename) {
    this.origFilename = origFilename;
  }

  @IndexAnnotation(fieldName = "dynamic_t_nameNLD", canBeEmpty = true, isFaceted = false)
  public String getNameNld() {
    return nameNld;
  }

  public void setNameNld(String name) {
    nameNld = name;
  }

  @IndexAnnotations({ @IndexAnnotation(fieldName = "dynamic_sort_name", canBeEmpty = true, isFaceted = false, isSortable = true),
      @IndexAnnotation(fieldName = "dynamic_t_nameEng", canBeEmpty = true, isFaceted = false, isSortable = false) })
  public String getNameEng() {
    return nameEng;
  }

  public void setNameEng(String name) {
    nameEng = name;
  }

  @IndexAnnotation(fieldName = "dynamic_s_begin_date", canBeEmpty = true, isFaceted = true, facetType = FacetType.DATE)
  public String getBeginDate() {
    return beginDate;
  }

  public void setBeginDate(String date) {
    beginDate = date;
  }

  @IndexAnnotation(fieldName = "dynamic_s_end_date", canBeEmpty = false, isFaceted = true, facetType = FacetType.DATE)
  public String getEndDate() {
    return endDate;
  }

  public void setEndDate(String date) {
    endDate = date;
  }

  @JsonIgnore
  @IndexAnnotations({ @IndexAnnotation(fieldName = "dynamic_k_period", canBeEmpty = true, isFaceted = false, facetType = FacetType.PERIOD, isSortable = true),
      @IndexAnnotation(fieldName = "dynamic_s_period", canBeEmpty = true, isFaceted = true, facetType = FacetType.PERIOD, isSortable = false) })
  public String getActivePeriod() {
    return PeriodHelper.createPeriod(beginDate, endDate);
  }

  public String getPeriodDescription() {
    return periodDescription;
  }

  public void setPeriodDescription(String description) {
    periodDescription = description;
  }

  @IndexAnnotation(fieldName = "dynamic_t_history", canBeEmpty = true, isFaceted = false)
  public String getHistory() {
    return history;
  }

  public void setHistory(String history) {
    this.history = history;
  }

  @JsonIgnore
  @IndexAnnotation(fieldName = "dynamic_s_archive", accessors = { "getDisplayName" }, canBeEmpty = true, isFaceted = false)
  public List<RelationRef> getRelatedArchives() {
    return getRelations(IS_CREATOR_OF.regular);
  }

  @JsonIgnore
  @IndexAnnotation(fieldName = "dynamic_s_related_creator", accessors = { "getDisplayName" }, canBeEmpty = true, isFaceted = false)
  public List<RelationRef> getRelatedArchivers() {
    return getRelations(HAS_SIBLING_ARCHIVER.regular);
  }

  @JsonIgnore
  @IndexAnnotation(fieldName = "dynamic_s_place", accessors = { "getDisplayName" }, canBeEmpty = true, isFaceted = true)
  public List<RelationRef> getPlaceKeywords() {
    return getRelations(HAS_ARCHIVER_PLACE.regular);
  }

  @JsonIgnore
  @IndexAnnotation(fieldName = "dynamic_s_subject", accessors = { "getDisplayName" }, canBeEmpty = true, isFaceted = true)
  public List<RelationRef> getSubjectKeywords() {
    return getRelations(HAS_ARCHIVER_KEYWORD.regular);
  }

  @JsonIgnore
  @IndexAnnotation(fieldName = "dynamic_s_person", accessors = { "getDisplayName" }, canBeEmpty = true, isFaceted = true)
  public List<RelationRef> getPersons() {
    return getRelations(HAS_ARCHIVER_PERSON.regular);
  }

  @IndexAnnotation(fieldName = "dynamic_t_notes", canBeEmpty = true, isFaceted = false)
  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getLiterature() {
    return literature;
  }

  public void setLiterature(String literature) {
    this.literature = literature;
  }

  public String getMadeBy() {
    return madeBy;
  }

  public void setMadeBy(String madeBy) {
    this.madeBy = madeBy;
  }

  public String getReminders() {
    return reminders;
  }

  public void setReminders(String reminders) {
    this.reminders = reminders;
  }

  public XRelated[] getRelated() {
    return related;
  }

  public void setRelated(XRelated[] related) {
    this.related = related;
  }

  @IndexAnnotation(fieldName = "dynamic_s_type", accessors = { "toString" }, canBeEmpty = true, isFaceted = true)
  public List<String> getTypes() {
    return types;
  }

  public void setTypes(List<String> types) {
    this.types = types;
  }

  public void addType(String type) {
    types.add(type);
  }

  @Override
  public Map<String, String> createRelSearchRep(Map<String, String> mappedIndexInformation) {
    Map<String, String> data = Maps.newTreeMap();

    addValueToMap(mappedIndexInformation, data, ID_PROPERTY_NAME);
    addValueToMap(mappedIndexInformation, data, "nameEng");
    addValueToMap(mappedIndexInformation, data, "beginDate");
    addValueToMap(mappedIndexInformation, data, "endDate");
    addValueToMap(mappedIndexInformation, data, "types");

    return data;
  }

  @Override
  public Map<String, String> getClientRepresentation() {
    Map<String, String> data = Maps.newTreeMap();
    addItemToRepresentation(data, ID_PROPERTY_NAME, getId());
    addItemToRepresentation(data, "nameEng", getNameEng());
    addItemToRepresentation(data, "beginDate", getBeginDate());
    addItemToRepresentation(data, "endDate", getEndDate());
    addItemToRepresentation(data, "types", getTypes());

    return data;
  }

}
