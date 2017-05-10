package net.atomique.ksar.ui;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class NaturalComparatorTest {
    private final String a;
    private final String b;
    private final int expected;

    public NaturalComparatorTest(String a, String b, int expected) {
        this.a = a;
        this.b = b;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "{0} vs {1} => {2}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"1", "2", -1},
                {"1", "10", -1},
                {"1", "100", -1},
                {"11", "10", 1},
                {"10", "10", 0},
                {"2", "1", 1},
                {"2", "10", -1},
                {"02", "10", -1},
                {"02", "1", 1},

                {"a", "b", -1},
                {"a", "aa", -1},
                {"c", "a", 1},
                {"d", "d", 0},

                {"qw42", "ab42", 1},
                {"qw42", "ab43", 1},
                {"ab42", "ab43", -1},
                {"ab420", "ab43", 1},
                {"ab42cd", "ab42", 1},
                {"ab42cd", "ab42cd", 0},
                {"ab42cd2", "ab42cd10", -1},

                {"cpu 2", "cpu10", -1},
                {"cpu 1core10", "cpu1 core 2", 1},

                {" cpu42", "  cpu     42   ", 0},
                {" cpu42", "  cpu     120   ", -1},

                {"cpu 2", "cpu2core3", -1},
                {"cpu 1 core 10 thread1", "cpu1core2thread2", 1},
                {"", "cpu", -1},

                {"all", "0", 1}
        });
    }

    @Test
    public void testNaturalOrder() {
        cmp(a, b, expected);
    }

    private void cmp(String a, String b, int expectedResult) {
        NaturalComparator c = new NaturalComparator();
        int res = c.compare(a, b);
        int exp = expectedResult < 0 ? -1 : (expectedResult == 0 ? 0 : 1);
        int act = res < 0 ? -1 : (res == 0 ? 0 : 1);

        assertEquals(a + " vs " + b, exp, act);
    }
}
