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

import java.util.Map;
import java.util.regex.Pattern;

import nl.knaw.huygens.timbuctoo.model.util.PersonNameComponent.Type;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * Supports building of a string representation of a name.
 */
public class PersonNameBuilder {

  private static final String EMPTY = "";
  private static final String SPACE = " ";
  private static final String COMMA = ", ";

  private static final Map<MultiKey, String> separators = createSeparatorMap();

  private static Map<MultiKey, String> createSeparatorMap() {
    Map<MultiKey, String> map = Maps.newHashMap();
    map.put(new MultiKey(Type.SURNAME, Type.FORENAME), COMMA);
    map.put(new MultiKey(Type.SURNAME, Type.GEN_NAME), COMMA);
    map.put(new MultiKey(Type.SURNAME, Type.ADD_NAME), COMMA);
    map.put(new MultiKey(Type.SURNAME, Type.NAME_LINK), COMMA);
    map.put(new MultiKey(Type.FORENAME, Type.ADD_NAME), COMMA);
    map.put(new MultiKey(Type.GEN_NAME, Type.ADD_NAME), COMMA);
    return map;
  }

  @VisibleForTesting
  static String separator(Type type1, Type type2) {
    if (type1 == null || type2 == null) {
      return EMPTY;
    } else {
      String value = separators.get(new MultiKey(type1, type2));
      return (value != null) ? value : SPACE;
    }
  }

  // -------------------------------------------------------------------

  private static final Pattern ELISIONS = Pattern.compile("\\b([dDlL]')\\s+");

  private final StringBuilder builder;
  private Type prev;

  public PersonNameBuilder() {
    builder = new StringBuilder();
    prev = null;
  }

  public void addComponent(PersonNameComponent component) {
    Type type = component.getType();
    builder.append(separator(prev, type));
    builder.append(component.getValue());
    prev = type;
  }

  public String getName() {
    return ELISIONS.matcher(builder).replaceAll("$1");
  }

  // -------------------------------------------------------------------

  /**
   * Combines two type values to form a single, composite key.
   */
  private static class MultiKey {
    private final Type key1;
    private final Type key2;

    public MultiKey(Type key1, Type key2) {
      this.key1 = Preconditions.checkNotNull(key1);
      this.key2 = Preconditions.checkNotNull(key2);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof MultiKey) {
        MultiKey that = (MultiKey) obj;
        return this.key1 == that.key1 && this.key2 == that.key2;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return 31 * key1.hashCode() + key2.hashCode();
    }
  }

}
