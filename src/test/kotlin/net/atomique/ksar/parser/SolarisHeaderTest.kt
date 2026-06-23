/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import net.atomique.ksar.kSar;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class SolarisHeaderTest {
  @ParameterizedTest
  @MethodSource("testValues")
  public void test(String header, String sarString, LocalDateTime expectedDate) {
    String[] columns = sarString.split("\\s+");
    kSar ksar = new kSar();
    SunOS sut = new SunOS();
    sut.init(ksar, header);
    sut.parse(sarString, columns);
    assertEquals(expectedDate, sut.getStartOfGraph(), () -> "header: " + header + ", sar string: " + sarString);
  }

  public static Stream<Arguments> testValues() {
    String str = "05/31/2018 00:05:01";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
    LocalDateTime dateTime = LocalDateTime.parse(str, formatter);

    return Stream.of(
        arguments("SunOS hostname.example.com 5.11 11.3 sun4v    05/31/2018",
            "00:05:01    %usr    %sys    %wio   %idle",
            dateTime));
  }

}
