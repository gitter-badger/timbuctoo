package test.tools.model;

/*
 * #%L
 * Timbuctoo tools
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

import nl.knaw.huygens.timbuctoo.model.DomainEntity;

import com.google.common.base.Objects;

public class BaseDomainEntity extends DomainEntity {

  public String name;
  public String generalTestDocValue;

  public BaseDomainEntity() {}

  public BaseDomainEntity(String id) {
    setId(id);
  }

  public BaseDomainEntity(String id, String name) {
    setId(id);
    this.name = name;
  }

  @Override
  public String getIdentificationName() {
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BaseDomainEntity)) {
      return false;
    }

    BaseDomainEntity other = (BaseDomainEntity) obj;

    boolean isEqual = true;

    isEqual &= Objects.equal(other.generalTestDocValue, generalTestDocValue);
    isEqual &= Objects.equal(other.getId(), getId());
    isEqual &= Objects.equal(other.getPid(), getPid());

    return isEqual;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(generalTestDocValue, getId(), getPid());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("GeneralTestDoc { \ngeneralTestDocValue: ").append(generalTestDocValue);
    sb.append("\nid: ").append(getId());
    sb.append("\npid: ").append(getPid());
    sb.append("\n}");
    return sb.toString();
  }

}
