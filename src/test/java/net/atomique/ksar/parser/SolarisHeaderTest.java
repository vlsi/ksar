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
public class SolarisHeaderTest {
  private String header;
  private String sarString;
  private LocalDateTime expectedDate;
  private SunOS sut;

  public SolarisHeaderTest(String header, String sarString, LocalDateTime expectedDate) {
    this.header = header;
    this.sarString = sarString;
    this.expectedDate = expectedDate;
  }

  @Before
  public void setUp() throws Exception {
    sut = new SunOS();
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
    String str = "05/31/2018 00:05:01";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
    LocalDateTime dateTime = LocalDateTime.parse(str, formatter);

    return Arrays.asList(new Object[][] {
        { "SunOS hostname.example.com 5.11 11.3 sun4v    05/31/2018",
            "00:05:01    %usr    %sys    %wio   %idle",
            dateTime },
    });
  }

}
