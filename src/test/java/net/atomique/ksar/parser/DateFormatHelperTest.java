/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.parser;

import org.junit.Ignore;
import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class DateFormatHelperTest {
  @Test
  @Ignore
  public void generateTests() throws Exception {
    Set<String> allFormats = new HashSet<>();
    LocalDate date = LocalDate.of(2017, 10, 18);
    Predicate<String> nonPunctuation = Pattern.compile("[^ ./-\\:0-9]{3,}").asPredicate();
    for (Locale locale : Locale.getAvailableLocales()) {
      for (FormatStyle style : EnumSet.of(FormatStyle.SHORT, FormatStyle.MEDIUM)) {
        DateTimeFormatter f = DateTimeFormatter.ofLocalizedDate(style).withLocale(locale);
        String str = f.format(date);
        if (nonPunctuation.test(str)) {
          continue;
        }
        String v = str.replaceAll("2017", "yyyy").replaceAll("17", "yy")
            .replaceAll("18", "dd").replaceAll("10", "MM");
        allFormats.add(v);
      }
    }
    List<String> formats = new ArrayList<>(allFormats);
    formats.sort(Comparator.<String, String>comparing(v -> v.replaceAll("[^\\w]", "-"))
        .thenComparing(Function.identity()));

    for (String format : formats) {
      System.out.println('"' + format + "\",");
    }
  }

}
