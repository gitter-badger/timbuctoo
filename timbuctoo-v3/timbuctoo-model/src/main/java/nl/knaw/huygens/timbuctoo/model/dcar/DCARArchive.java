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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import nl.knaw.huygens.facetedsearch.model.FacetType;
import nl.knaw.huygens.timbuctoo.facet.IndexAnnotation;
import nl.knaw.huygens.timbuctoo.facet.IndexAnnotations;
import nl.knaw.huygens.timbuctoo.model.Archive;
import nl.knaw.huygens.timbuctoo.model.RelationRef;

import java.util.List;
import java.util.Map;

import static nl.knaw.huygens.timbuctoo.model.dcar.RelTypeNames.HAS_ARCHIVE_KEYWORD;
import static nl.knaw.huygens.timbuctoo.model.dcar.RelTypeNames.HAS_ARCHIVE_PERSON;
import static nl.knaw.huygens.timbuctoo.model.dcar.RelTypeNames.HAS_ARCHIVE_PLACE;
import static nl.knaw.huygens.timbuctoo.model.dcar.RelTypeNames.HAS_PARENT_ARCHIVE;
import static nl.knaw.huygens.timbuctoo.model.dcar.RelTypeNames.HAS_SIBLING_ARCHIVE;
import static nl.knaw.huygens.timbuctoo.model.dcar.RelTypeNames.IS_CREATOR_OF;

public class DCARArchive extends Archive {

  /**
   * Migration: Name of source file
   */
  private String origFilename;

  /**
   * ING Forms: "Ref. code country"; refcode facet
   */
  private List<String> countries;

  /**
   * ING Forms: "Ref. code repository"; refcode facet
   */
  private String refCodeArchive;

  /**
   * ING Forms: "Reference code"
   */
  private String refCode;

  /**
   * ING Forms: "Code or indication of sub-fonds"
   */
  private String subCode;

  /**
   * ING Forms: "Indication of series, Nos."
   */
  private String series;

  /**
   * ING Forms: "Item, No."
   */
  private String itemNo;

  /**
   * ING Forms: "Title"
   */
  private String titleNld;

  /**
   * ING Forms: "English title"; text searchable
   */
  private String titleEng;

  /**
   * ING Forms: "Begin date"; date facet
   */
  private String beginDate;

  /**
   * ING Forms: "End date"; date facet
   */
  private String endDate;

  /**
   * ING Forms: "Period description"
   */
  private String periodDescription;

  /**
   * ING Forms: "Extent"
   */
  private String extent;

  /**
   * ING Forms: "Additional finding aid"
   */
  private String findingAid;

  /** ING Forms: "Name(s) of Creator(s)"; as relation */

  /**
   * ING Forms: "Scope and content"
   */
  private String scope;

  /** ING Forms: "Keyword(s) geography"; as relation; place facet */

  /** ING Forms: "Keyword(s) subject"; as relation; subject facet */

  /** ING Forms: "Keyword(s) person"; as relation; person facet */

  /**
   * ING Forms: "Remarks"; text searchable
   */
  private String notes;

  /**
   * ING Forms: "Record made by-"
   */
  private String madeBy;

  /**
   * ING Forms: "Reminders"
   */
  private String reminders;

  /** ING Forms: "Title related overhead level of description; as relation" */

  /** ING Forms: "Title(s) related underlying level(s) of description; as relation" */

  /**
   * ING Forms: "Other related units of description; as relation"
   */

  public DCARArchive() {
    countries = Lists.newArrayList();
  }

  @Override
  public String getIdentificationName() {
    return getTitleEng();
  }

  public String getOrigFilename() {
    return origFilename;
  }

  public void setOrigFilename(String origFilename) {
    this.origFilename = origFilename;
  }

  @IndexAnnotation(fieldName = "dynamic_s_countries", canBeEmpty = true)
  public List<String> getCountries() {
    return countries;
  }

  public void setCountries(List<String> countries) {
    this.countries = countries;
  }

  public void addCountry(String country) {
    countries.add(country);
  }

  @IndexAnnotation(fieldName = "dynamic_s_refCodeArchive", canBeEmpty = true)
  public String getRefCodeArchive() {
    return refCodeArchive;
  }

  public void setRefCodeArchive(String refCodeArchive) {
    this.refCodeArchive = refCodeArchive;
  }

  @IndexAnnotation(fieldName = "dynamic_s_refCode", canBeEmpty = true)
  public String getRefCode() {
    return refCode;
  }

  public void setRefCode(String refCode) {
    this.refCode = refCode;
  }

  @IndexAnnotation(fieldName = "dynamic_s_subCode", canBeEmpty = true)
  public String getSubCode() {
    return subCode;
  }

  public void setSubCode(String subCode) {
    this.subCode = subCode;
  }

  @JsonIgnore
  @IndexAnnotation(fieldName = "dynamic_s_refcode", canBeEmpty = true, isFaceted = true)
  public String getIndexedRefCode() {
    StringBuilder builder = new StringBuilder();
    for (String country : getCountries()) {
      appendTo(builder, country);
    }
    appendTo(builder, getRefCodeArchive());
    return builder.toString();
  }

  private void appendTo(StringBuilder builder, String text) {
    if (text != null && text.length() != 0) {
      if (builder.length() != 0) {
        builder.append(' ');
      }
      builder.append(text);
    }
  }

  @IndexAnnotation(fieldName = "dynamic_s_series", canBeEmpty = true)
  public String getSeries() {
    return series;
  }

  public void setSeries(String series) {
    this.series = series;
  }

  @IndexAnnotation(fieldName = "dynamic_s_itemNo", canBeEmpty = true)
  public String getItemNo() {
    return itemNo;
  }

  public void setItemNo(String itemNo) {
    this.itemNo = itemNo;
  }

  @IndexAnnotation(fieldName = "dynamic_t_titleNLD", canBeEmpty = true, isFaceted = false)
  public String getTitleNld() {
    return titleNld;
  }

  public void setTitleNld(String title) {
    titleNld = title;
  }

  @IndexAnnotations({
    @IndexAnnotation(fieldName = "dynamic_sort_title", canBeEmpty = true, isFaceted = false, isSortable = true),
    @IndexAnnotation(fieldName = "dynamic_t_titleEng", canBeEmpty = true, isFaceted = false, isSortable = false)})
  public String getTitleEng() {
    return titleEng;
  }

  public void setTitleEng(String title) {
    titleEng = title;
  }

  @IndexAnnotation(fieldName = "dynamic_s_begin_date", canBeEmpty = true, isFaceted = true, facetType = FacetType.DATE)
  public String getBeginDate() {
    return beginDate;
  }

  public void setBeginDate(String date) {
    beginDate = date;
  }

  @IndexAnnotation(fieldName = "dynamic_s_end_date", canBeEmpty = true, isFaceted = true, facetType = FacetType.DATE)
  public String getEndDate() {
    return endDate;
  }

  public void setEndDate(String date) {
    endDate = date;
  }

  @JsonIgnore
  @IndexAnnotations({
    @IndexAnnotation(fieldName = "dynamic_k_period", canBeEmpty = true, isFaceted = false, facetType = FacetType
      .PERIOD, isSortable = true),
    @IndexAnnotation(fieldName = "dynamic_s_period", canBeEmpty = true, isFaceted = true, facetType = FacetType
      .PERIOD, isSortable = false)})
  public String getActivePeriod() {
    return PeriodHelper.createPeriod(beginDate, endDate);
  }

  public String getPeriodDescription() {
    return periodDescription;
  }

  public void setPeriodDescription(String description) {
    periodDescription = description;
  }

  public String getExtent() {
    return extent;
  }

  public void setExtent(String extent) {
    this.extent = extent;
  }

  public String getFindingAid() {
    return findingAid;
  }

  public void setFindingAid(String findingAid) {
    this.findingAid = findingAid;
  }

  @JsonIgnore
  @IndexAnnotation(fieldName = "dynamic_s_creator", accessors = {
    "getDisplayName"}, canBeEmpty = true, isFaceted = false)
  public List<RelationRef> getCreators() {
    return getRelations(IS_CREATOR_OF.inverse);
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  @JsonIgnore
  @IndexAnnotation(fieldName = "dynamic_s_place", accessors = {"getDisplayName"}, canBeEmpty = true, isFaceted = true)
  public List<RelationRef> getPlaceKeywords() {
    return getRelations(HAS_ARCHIVE_PLACE.regular);
  }

  @JsonIgnore
  @IndexAnnotation(fieldName = "dynamic_s_subject", accessors = {"getDisplayName"}, canBeEmpty = true, isFaceted = true)
  public List<RelationRef> getSubjectKeywords() {
    return getRelations(HAS_ARCHIVE_KEYWORD.regular);
  }

  @JsonIgnore
  @IndexAnnotation(fieldName = "dynamic_s_person", accessors = {"getDisplayName"}, canBeEmpty = true, isFaceted = true)
  public List<RelationRef> getPersons() {
    return getRelations(HAS_ARCHIVE_PERSON.regular);
  }

  @IndexAnnotation(fieldName = "dynamic_t_notes", canBeEmpty = true, isFaceted = false)
  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
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

  @JsonIgnore
  @IndexAnnotation(fieldName = "dynamic_s_related_archive", accessors = {
    "getDisplayName"}, canBeEmpty = true, isFaceted = false)
  public List<RelationRef> getOverheadArchives() {
    return getRelations(HAS_PARENT_ARCHIVE.regular);
  }

  @JsonIgnore
  @IndexAnnotation(fieldName = "dynamic_s_related_archive", accessors = {
    "getDisplayName"}, canBeEmpty = true, isFaceted = false)
  public List<RelationRef> getUnderlyingArchives() {
    return getRelations(HAS_PARENT_ARCHIVE.inverse);
  }

  @JsonIgnore
  @IndexAnnotation(fieldName = "dynamic_s_related_archive", accessors = {
    "getDisplayName"}, canBeEmpty = true, isFaceted = false)
  public List<RelationRef> getRelatedUnitArchives() {
    return getRelations(HAS_SIBLING_ARCHIVE.regular);
  }

  @Override
  public Map<String, String> createRelSearchRep(Map<String, String> mappedIndexInformation) {
    Map<String, String> data = Maps.newTreeMap();

    addValueToMap(mappedIndexInformation, data, ID_PROPERTY_NAME);
    addValueToMap(mappedIndexInformation, data, "titleEng");
    addValueToMap(mappedIndexInformation, data, "beginDate");
    addValueToMap(mappedIndexInformation, data, "endDate");
    addValueToMap(mappedIndexInformation, data, "countries");
    addValueToMap(mappedIndexInformation, data, "refCodeArchive");
    addValueToMap(mappedIndexInformation, data, "refCode");
    addValueToMap(mappedIndexInformation, data, "subCode");
    addValueToMap(mappedIndexInformation, data, "itemNo");
    addValueToMap(mappedIndexInformation, data, "series");

    return data;
  }

  @Override
  public Map<String, String> getClientRepresentation() {
    Map<String, String> data = Maps.newTreeMap();
    addItemToRepresentation(data, ID_PROPERTY_NAME, getId());
    addItemToRepresentation(data, "titleEng", getTitleEng());
    addItemToRepresentation(data, "beginDate", getBeginDate());
    addItemToRepresentation(data, "endDate", getEndDate());
    addItemToRepresentation(data, "countries", getCountries());
    addItemToRepresentation(data, "refCodeArchive", getRefCodeArchive());
    addItemToRepresentation(data, "refCode", getRefCode());
    addItemToRepresentation(data, "subCode", getSubCode());
    addItemToRepresentation(data, "itemNo", getItemNo());
    addItemToRepresentation(data, "series", getSeries());

    return data;
  }
}
