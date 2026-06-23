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

public class HpuxHeaderTest {
  @ParameterizedTest
  @MethodSource("testValues")
  public void test(String header, String sarString, LocalDateTime expectedDate) {
    HPUX sut = new HPUX();
    String[] columns = sarString.split("\\s+");
    kSar ksar = new kSar();
    sut.init(ksar, header);
    sut.parse(sarString, columns);
    assertEquals(expectedDate, sut.getStartOfGraph(), () -> "header: " + header + ", sar string: " + sarString);
  }

  public static Stream<Arguments> testValues() {
    String str = "2018-03-21 00:05:01";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime dateTime = LocalDateTime.parse(str, formatter);

    return Stream.of(
        arguments(
            "HP-UX hostname.example.com B.11.31 U ia64    03/21/18",
            "00:05:01      19       8      29      45",
            dateTime));
  }
}
