
package net.atomique.ksar.Graph;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

public class IEEE1541Number extends NumberFormat {


    public IEEE1541Number() {
    }

    public IEEE1541Number(int value) {
        kilo = value;
    }

    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        if (kilo == 0) {
            return toAppendTo.append(number);
        }
        if ((number * kilo) < 1024) {
            return toAppendTo.append(number);
        }
        if ((number * kilo) < (1024 * 1024)) {
            DecimalFormat formatter = new DecimalFormat("#,##0.0");
            toAppendTo.append(formatter.format((double) number / 1024.0)).append(" KB");
            return toAppendTo;
        }
        if ((number * kilo) < (1024 * 1024 * 1024)) {
            DecimalFormat formatter = new DecimalFormat("#,##0.0");
            toAppendTo.append(formatter.format((double) (number * kilo) / (1024.0 * 1024.0))).append(" MB");
            return toAppendTo;
        }

        DecimalFormat formatter = new DecimalFormat("#,##0.0");
        toAppendTo.append(formatter.format((double) (number * kilo) / (1024.0 * 1024.0 * 1024.0))).append(" GB");
        return toAppendTo;
    }

    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return format((double) (number * kilo), toAppendTo, pos);
    }

    public Number parse(String source, ParsePosition parsePosition) {
        return null;
    }
    
    int kilo = 0;
}
