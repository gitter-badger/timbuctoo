package nl.knaw.huygens.timbuctoo.model;

/*
 * #%L
 * Timbuctoo core
 * =======
 * Copyright (C) 2012 - 2013 Huygens ING
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

import nl.knaw.huygens.timbuctoo.annotations.IDPrefix;
import nl.knaw.huygens.timbuctoo.facet.IndexAnnotation;

@IDPrefix("PLAC")
public class Place extends DomainEntity {

  public String name;
  public String latitude;
  public String longitude;

  @Override
  public String getDisplayName() {
    return name;
  };

  @IndexAnnotation(fieldName = "dynamic_t_name", isFaceted = false)
  public String getName() {
    return name;
  }

  @IndexAnnotation(fieldName = "dynamic_s_latitude")
  public String getLatitude() {
    return latitude;
  }

  @IndexAnnotation(fieldName = "dynamic_s_longitude")
  public String getLongitude() {
    return longitude;
  }

}
