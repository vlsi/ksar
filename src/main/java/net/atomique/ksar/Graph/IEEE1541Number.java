
package net.atomique.ksar.Graph;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

public class IEEE1541Number extends NumberFormat {

    private final static double IEC_kibi = 1024.0;
    private final static double IEC_mebi = 1048576.0;
    private final static double IEC_gibi = 1073741824.0;

/*    public IEEE1541Number() {
    }*/

    public IEEE1541Number(int value) {
        kilo = value;
    }

    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        DecimalFormat formatter = new DecimalFormat("#,##0.0");

        if (kilo == 0) {
            return toAppendTo.append(formatter.format( number));
        }
        if ((number * kilo) < IEC_kibi) {
            return toAppendTo.append(formatter.format( number));
        }
        if ((number * kilo) < IEC_mebi) {

            toAppendTo.append(formatter.format( (number * kilo)/ IEC_kibi)).append(" Ki");
            return toAppendTo;
        }
        if ((number * kilo) < (IEC_gibi)) {
            toAppendTo.append(formatter.format((number * kilo) / (IEC_mebi))).append(" Mi");
            return toAppendTo;
        }

        toAppendTo.append(formatter.format((number * kilo) / (IEC_gibi))).append(" Gi");
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
