package nl.knaw.huygens.timbuctoo.vre;

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import nl.knaw.huygens.timbuctoo.model.Person;
import nl.knaw.huygens.timbuctoo.model.Place;
import nl.knaw.huygens.timbuctoo.model.User;
import nl.knaw.huygens.timbuctoo.model.dcar.DCARPerson;

import org.junit.BeforeClass;
import org.junit.Test;

public class DutchCaribbeanScopeTest {

  private static Scope scope;

  @BeforeClass
  public static void setupScope() throws IOException {
    scope = new DutchCaribbeanScope();
  }

  @Test
  public void testBaseEntityTypes() {
    assertTrue(scope.getBaseEntityTypes().contains(Person.class));
    assertFalse(scope.getBaseEntityTypes().contains(Place.class));
    assertFalse(scope.getBaseEntityTypes().contains(User.class));
    assertFalse(scope.getBaseEntityTypes().contains(DCARPerson.class));
  }

  @Test
  public void testAllEntityTypes() {
    assertFalse(scope.getAllEntityTypes().contains(Person.class));
    assertFalse(scope.getAllEntityTypes().contains(Place.class));
    assertFalse(scope.getAllEntityTypes().contains(User.class));
    assertTrue(scope.getAllEntityTypes().contains(DCARPerson.class));
  }

  @Test
  public void testTypeAndIdInScope() {
    assertFalse(scope.inScope(Person.class, "id"));
    assertFalse(scope.inScope(Place.class, "id"));
    assertTrue(scope.inScope(DCARPerson.class, "id"));
  }

  @Test
  public void testInstanceInScopeBy() {
    assertFalse(scope.inScope(new Person()));
    assertFalse(scope.inScope(new Place()));
    assertTrue(scope.inScope(new DCARPerson()));
  }

}
