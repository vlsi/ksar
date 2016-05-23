package net.atomique.ksar.UI;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SimpleNaturalComparatorTest {
    @Test
    public void testCompareNumbers() {
        SortedTreeNode.SimpleNaturalComparator comparator = new SortedTreeNode.SimpleNaturalComparator();

        assertTrue(comparator.compare("10", "2") > 0);
        assertTrue(comparator.compare("a10", "a2") < 0);
        assertTrue(comparator.compare("a", "1") > 0);
    }
}
