package nl.knaw.huygens.timbuctoo.model.util;

/*
 * #%L
 * Timbuctoo core
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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import nl.knaw.huygens.timbuctoo.model.util.PersonNameComponent.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * A person name consisting of components (the order is important).
 * Based on the TEI description of person names.
 */
public class PersonName {

  public static PersonName newInstance(String forename, String surname) {
    PersonName name = new PersonName();
    name.addNameComponent(PersonNameComponent.Type.FORENAME, forename);
    name.addNameComponent(PersonNameComponent.Type.SURNAME, surname);
    return name;
  }

  private static final Set<Type> ALL = EnumSet.allOf(Type.class);
  private static final Set<Type> SHORT = EnumSet.of(Type.FORENAME, Type.SURNAME, Type.NAME_LINK);

  private List<PersonNameComponent> components;

  public PersonName() {
    components = Lists.newArrayList();
  }

  public List<PersonNameComponent> getComponents() {
    return components;
  }

  public void setComponents(List<PersonNameComponent> components) {
    this.components = components;
  }

  public void addNameComponent(Type type, String value) {
    if (value != null && value.length() > 0) {
      components.add(new PersonNameComponent(type, value));
    }
  }

  @JsonIgnore
  public String getFullName() {
    PersonNameBuilder builder = new PersonNameBuilder();
    appendTo(builder, ALL);
    return builder.getName();
  }

  @JsonIgnore
  public String getShortName() {
    PersonNameBuilder builder = new PersonNameBuilder();
    appendTo(builder, SHORT);
    return builder.getName();
  }

  @JsonIgnore
  public String getSortName() {
    PersonNameBuilder builder = new PersonNameBuilder();
    int surnames = appendTo(builder, EnumSet.of(Type.SURNAME));
    appendForenames(builder);
    appendTo(builder, EnumSet.of(Type.NAME_LINK));
    if (surnames == 0) {
      appendTo(builder, EnumSet.of(Type.ADD_NAME));
    }
    return builder.getName();
  }

  private int appendTo(PersonNameBuilder builder, Set<Type> types) {
    int count = 0;
    for (PersonNameComponent component : components) {
      if (types.contains(component.getType())) {
        builder.addComponent(component);
        count++;
      }
    }
    return count;
  }

  private void appendForenames(PersonNameBuilder builder) {
    Type prev = null;
    for (PersonNameComponent component : components) {
      Type type = component.getType();
      if (type == Type.FORENAME || (type == Type.GEN_NAME && prev == Type.FORENAME)) {
        builder.addComponent(component);
      }
      prev = type;
    }
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof PersonName) {
      return Objects.equal(components, ((PersonName) object).components);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(components);
  }

  @Override
  public String toString() {
    return getFullName();
  }

}
