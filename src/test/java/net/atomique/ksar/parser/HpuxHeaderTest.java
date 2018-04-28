/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.parser;

import static org.junit.Assert.assertEquals;

import net.atomique.ksar.kSar;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class HpuxHeaderTest {
  private String header;
  private String sarString;
  private LocalDateTime expectedDate;
  private HPUX sut;

  public HpuxHeaderTest(String header, String sarString, LocalDateTime expectedDate) {
    this.header = header;
    this.sarString = sarString;
    this.expectedDate = expectedDate;
  }

  @Before
  public void setUp() throws Exception {
    sut = new HPUX();
  }

  @Test
  public void test() {
    String[] columns = sarString.split("\\s+");
    kSar ksar = new kSar();
    sut.init(ksar, header);
    sut.parse(sarString, columns);
    assertEquals(expectedDate, sut.get_startofgraph());
  }

  @Parameters
  public static Collection testValues() {

    // current Byte, expected value, expected unit, test name
    String str = "2018-03-21 00:05:01";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime dateTime = LocalDateTime.parse(str, formatter);

    return Arrays.asList(new Object[][] {
        { "HP-UX hostname.example.com B.11.31 U ia64    03/21/18",
            "00:05:01      19       8      29      45",
            dateTime },
    });
  }

}
