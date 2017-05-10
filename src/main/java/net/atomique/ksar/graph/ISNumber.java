/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.graph;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

public class ISNumber extends NumberFormat {

    private final static double IS_kilo = 1000.0;
    private final static double IS_mega = 1000000.0;
    private final static double IS_giga = 1000000000.0;

/*    public ISNumber() {
    }*/

    public ISNumber(int value) {
        kilo = value;
    }

    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        DecimalFormat formatter = new DecimalFormat("#,##0.0");

        if (kilo == 0) {
            return toAppendTo.append(formatter.format( number));
        }
        if ((number * kilo) < IS_kilo) {
            return toAppendTo.append(formatter.format( number));
        }
        if ((number * kilo) < IS_mega) {

            toAppendTo.append(formatter.format( (number * kilo) / IS_kilo)).append(" K");
            return toAppendTo;
        }
        if ((number * kilo) < (IS_giga)) {
            toAppendTo.append(formatter.format((number * kilo) / (IS_mega))).append(" M");
            return toAppendTo;
        }

        toAppendTo.append(formatter.format((number * kilo) / (IS_giga))).append(" G");
        return toAppendTo;
    }

    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return format((double) (number * kilo), toAppendTo, pos);
    }

    public Number parse(String source, ParsePosition parsePosition) {
        return null;
    }
    
    private int kilo = 0;
}
