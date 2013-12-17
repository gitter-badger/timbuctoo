package test.model.projecta;

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

import test.model.TestRole;

public class TestRoleA2 extends TestRole {

  private String propertyA2;

  public TestRoleA2() {}

  public TestRoleA2(String property, String propertyA2) {
    super(property);
    setPropertyA2(propertyA2);
  }

  public String getPropertyA2() {
    return propertyA2;
  }

  public void setPropertyA2(String property) {
    propertyA2 = property;
  }

}
