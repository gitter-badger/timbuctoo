package nl.knaw.huygens.timbuctoo.storage;

/*
 * #%L
 * Timbuctoo core
 * =======
 * Copyright (C) 2012 - 2014 Huygens ING
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import nl.knaw.huygens.timbuctoo.config.TypeNames;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;

/**
 * A class that contains all the information about how the class fields are mapped 
 * to the fields in the database.
 */
public class FieldMapper {

  /** Separator between parts of a property name, as string. */
  public static final String SEPARATOR = ":";

  /** Separator between parts of a key, as character. */
  public static final char SEPARATOR_CHAR = ':';

  /** Returns the name of a property from its parts. */
  public static String propertyName(String prefix, String field) {
    checkArgument(field != null && field.length() != 0);

    StringBuilder builder = new StringBuilder();
    if (Character.isLetter(field.charAt(0))) {
      builder.append(prefix).append(SEPARATOR_CHAR);
    }
    builder.append(field);
    return builder.toString();
  }

  /** Returns the name of a property from its parts. */
  public static String propertyName(Class<?> type, String field) {
    return propertyName(TypeNames.getInternalName(type), field);
  }

  // -------------------------------------------------------------------

  /**
   * Adds declared fields of the specified view type to the field map,
   * using as keys the corresponding property names.
   */
  public void addToFieldMap(Class<?> prefixType, Class<?> viewType, Map<String, Field> map) {
    String prefix = TypeNames.getInternalName(prefixType);
    for (Field field : viewType.getDeclaredFields()) {
      if (isProperty(field)) {
        String fieldName = getFieldName(viewType, field);
        if (fieldName.indexOf('@') < 0) { // exclude virtual properties
          map.put(propertyName(prefix, fieldName), field);
        }
      }
    }
  }

  /**
   * Returns a field map for the specified view type with the specified prefix.
   */
  public Map<String, Field> getSimpleFieldMap(Class<?> prefixType, Class<?> viewType) {
    Map<String, Field> map = Maps.newHashMap();
    addToFieldMap(prefixType, viewType, map);
    return map;
  }

  /**
   * Returns a composite field map for all types starting with ViewType up to and
   * including the stoptype. To get a non-empty map stopType must be a superclass
   * of viewType.
   */
  public Map<String, Field> getCompositeFieldMap(Class<?> prefixType, Class<?> viewType, Class<?> stopType) {
    Map<String, Field> map = Maps.newHashMap();
    Class<?> type = viewType;
    while (stopType.isAssignableFrom(type)) {
      addToFieldMap(prefixType, type, map);
      type = type.getSuperclass();
    }
    return map;
  }

  // -------------------------------------------------------------------

  /**
   * Generates a mapping of the declared fields of the specified type
   * to a property name with a specified prefix type.
   */
  public Map<String, String> getFieldMap(Class<?> prefixType, Class<?> type) {
    Map<String, String> map = Maps.newHashMap();
    for (Field field : type.getDeclaredFields()) {
      if (isProperty(field)) {
        String fieldName = getFieldName(type, field);
        if (fieldName.indexOf('@') < 0) { // exclude virtual properties
          map.put(field.getName(), propertyName(prefixType, fieldName));
        }
      }
    }
    return map;
  }

  /**
   * Indicates whether a field qualifies as property.
   */
  private boolean isProperty(Field field) {
    return (field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) == 0;
  }

  /**
   * Checks if there is variation possible for this field.
   */
  public boolean isFieldWithVariation(String fieldName) {
    return fieldName.contains(SEPARATOR);
  }

  /**
   * A method to retrieve the name of the type the field belongs to.
   * @param fieldName the field to retrieve the type name from.
   * @return the type name or {@code null} if the field does not belong to a type.
   * @throws NullPointerException is thrown if {@code fieldName} is {@code null}.
   */
  public String getTypeNameOfFieldName(String fieldName) {
    checkNotNull(fieldName);
    int pos = fieldName.indexOf(SEPARATOR_CHAR);
    return (pos < 0) ? null : fieldName.substring(0, pos);
  }

  /**
   * Gets the name of the specified field in the specified class, without a prefix.
   * It uses the name specified in {@code JsonProperty} annotations on the field
   * itself or the getter corresponding to the field (in that order).
   */
  public String getFieldName(Class<?> type, Field field) {
    JsonProperty annotation = field.getAnnotation(JsonProperty.class);
    if (annotation != null) {
      return annotation.value();
    }

    Method method = getMethodByName(type, getMethodName(field));
    if (method != null && method.getAnnotation(JsonProperty.class) != null) {
      return method.getAnnotation(JsonProperty.class).value();
    }

    return field.getName();
  }

  /**
   * Searches for a public method in the specified class or its superclasses
   * and -interfaces that matches the specified name and has no parameters.
   */
  private Method getMethodByName(Class<?> type, String methodName) {
    try {
      // TODO decide: use type.getDeclaredMethod(methodName)?
      return type.getMethod(methodName);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private static final String GET_ACCESSOR = "get";
  private static final String IS_ACCESSOR = "is"; // get accesor for booleans.

  private String getMethodName(Field field) {
    char[] fieldNameChars = field.getName().toCharArray();

    fieldNameChars[0] = Character.toUpperCase(fieldNameChars[0]);

    String accessor = isBoolean(field.getType()) ? IS_ACCESSOR : GET_ACCESSOR;
    return accessor.concat(String.valueOf(fieldNameChars));
  }

  private boolean isBoolean(Class<?> cls) {
    return cls == boolean.class || cls == Boolean.class;
  }

}
