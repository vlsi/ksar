/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.ui;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares strings in "human natural order".
 * E.g. {@code "cpu 2" < "cpu 10"}, and {@code "cpu 1 core 10 thread1" >  "cpu1core2thread2"}
 */
public class NaturalComparator implements Comparator<String> {
  public final static Comparator<String> INSTANCE = new NaturalComparator();
  public final static Comparator<String> NULLS_FIRST = Comparator.nullsFirst(INSTANCE);

  private final static Pattern WORD_PATTERN = Pattern.compile("\\s*+([^0-9\\s]++|\\d++)");

  @Override
  public int compare(String a, String b) {
    if (a == null || b == null) {
      return 0; // nulls should be handled in other comparator
    }

    Matcher ma = WORD_PATTERN.matcher(a);
    Matcher mb = WORD_PATTERN.matcher(b);

    while (true) {
      boolean findA = ma.find();
      boolean findB = mb.find();
      if (findA && !findB) {
        return 1;
      }
      if (!findA) {
        return findB ? -1 : 0;
      }
      String u = ma.group(1);
      String v = mb.group(1);

      if (Character.isDigit(u.charAt(0)) && Character.isDigit(v.charAt(0))) {
        int res = Integer.compare(u.length(), v.length());
        if (res != 0) {
          // The shorter the length the smaller the number
          return res;
        }
      }
      // Ether both are numeric of equal length (see above if)
      // or they are non-numeric, then we compare as strings
      // or one of them is numeric and another is not, then we compare as strings anyway
      int res = u.compareTo(v);
      if (res != 0) {
        return res;
      }
    }
  }
}
