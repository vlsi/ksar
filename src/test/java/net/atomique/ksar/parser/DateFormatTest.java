/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.parser;

import net.atomique.ksar.AllParser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class DateFormatTest {
  private final String text;
  private final LocalDate date;
  private final String expected;

  public DateFormatTest(LocalDate date, String text, String expected) {
    this.text = text;
    this.date = date;
    this.expected = expected;
  }

  @Parameterized.Parameters(name = "{1} -> {2}")
  public static Iterable<Object[]> params() {
    Collection<Object[]> res = new ArrayList<>();

    // See DateTest.generateFormats
    LocalDate date = LocalDate.of(2017, 5, 16);
    for (String format : Arrays.asList(
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
      DateTimeFormatter df = DateTimeFormatter.ofPattern(format);
      res.add(new Object[]{date, df.format(date), format});
    }
    // See https://github.com/vlsi/ksar/issues/103
    LocalDate aug_04_2017 = LocalDate.of(2017, 8, 4);
    res.add(new Object[]{aug_04_2017, "04/08/17", "dd/MM/yy"});
    return res;
  }

  @Test
  public void run() {
    DateTimeFormatter format = AllParser.determineDateFormat(text);
    LocalDate date = LocalDate.parse(text, format);
    Assert.assertEquals(text, this.date, date);
  }

}
