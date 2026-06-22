/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import net.atomique.ksar.AllParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class DateFormatTest {

  @ParameterizedTest(name = "{1} -> {2}")
  @MethodSource("dates")
  public void detectFormat(LocalDate expected, String text, String pattern) {
    DateTimeFormatter format = AllParser.determineDateFormat(text);
    LocalDate actual = LocalDate.parse(text, format);
    assertEquals(expected, actual, () -> "determineDateFormat(\"" + text + "\"), expected pattern " + pattern);
  }

  public static Stream<Arguments> dates() {
    List<Arguments> res = new ArrayList<>();

    // The day is 16 on purpose: it is greater than 12, so day and month cannot be swapped.
    // See DateFormatHelperTest.generateTests for how the pattern list was derived.
    LocalDate date = LocalDate.of(2017, 5, 16);
    for (String pattern : Arrays.asList(
        "MM-dd-yy",
        "MM/dd/yy",
        "dd-MM-yy",
        "dd.MM.yy",
        "dd/MM/yy",
        "dd.MM.yy.",
        "dd-MM-yyyy",
        "dd.MM.yyyy",
        "dd/MM/yyyy",
        "yy. MM. dd",
        "yy-MM-dd",
        "yy.MM.dd",
        "yy/MM/dd",
        "yy年MM月dd日",
        "yy.dd.MM",
        "yyyy-MM-dd",
        "yyyy.MM.dd",
        "yyyy/MM/dd",
        "yyyy.MM.dd."
    )) {
      DateTimeFormatter df = DateTimeFormatter.ofPattern(pattern);
      res.add(arguments(date, df.format(date), pattern));
    }
    // 04/08/17 is 4 August 2017 in dd/MM/yy. See https://github.com/vlsi/ksar/issues/103
    res.add(arguments(LocalDate.of(2017, 8, 4), "04/08/17", "dd/MM/yy"));
    // 31/12/23 matches dd/MM/yy (2023) and yy/MM/dd (2031); prefer the non-future 2023.
    res.add(arguments(LocalDate.of(2023, 12, 31), "31/12/23", "dd/MM/yy"));
    return res.stream();
  }

}
