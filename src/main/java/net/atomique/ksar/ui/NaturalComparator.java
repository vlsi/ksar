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

    private final static Pattern WORD_PATTERN = Pattern.compile("\\s*+([^0-9\\s]|\\d+)");

    @Override
    public int compare(String a, String b) {
        if (a == null || b == null) {
            return 0; // nulls should be handled in other comparator
        }

        Matcher ma = WORD_PATTERN.matcher(a);
        Matcher mb = WORD_PATTERN.matcher(b);

        for (; ma.find() && mb.find(); ) {
            String u = ma.group(1);
            String v = mb.group(1);
            boolean isDigit = Character.isDigit(u.charAt(0));
            if (!isDigit || !Character.isDigit(v.charAt(0))) {
                int res = u.compareTo(v);
                if (res != 0) {
                    return res;
                }
                continue;
            }

            long aLong = Long.parseLong(u);
            long bLong = Long.parseLong(v);
            if (aLong != bLong) {
                return Long.compare(aLong, bLong);
            }
        }

        return ma.find() ? 1 : (mb.find() ? -1 : 0);
    }
}
