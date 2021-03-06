package nl.knaw.huygens.timbuctoo.tools.util;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class EncodingFixerTest {

  @org.junit.Test
  public void testConversion() {
    EncodingFixer fixer = new EncodingFixer();
    assertThat(fixer.convert1("BelvÃ©dÃ¨re"), equalTo("Belvédère"));
    assertThat(fixer.convert1("CuraÃ§ao"), equalTo("Curaçao"));
    assertThat(fixer.convert1("WolffenbÃ¼ttel"), equalTo("Wolffenbüttel"));
    assertThat(fixer.convert1("notariÃ«le"), equalTo("notariële"));
  }

  @org.junit.Test
  public void testInvariance() {
    EncodingFixer fixer = new EncodingFixer();
    assertThat(fixer.convert1("Belvédère"), equalTo("Belvédère"));
    assertThat(fixer.convert1("Curaçao"), equalTo("Curaçao"));
  }

}
