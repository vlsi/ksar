/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class NaturalComparatorTest {
    @ParameterizedTest
    @MethodSource("data")
    fun testNaturalOrder(a: String, b: String, expected: Int) {
        val res = NaturalComparator.compare(a, b)
        assertEquals(expected, Integer.signum(res)) { "$a vs $b" }
    }

    companion object {
        @JvmStatic
        fun data(): Stream<Arguments> {
            return Stream.of(
                arguments("1", "2", -1),
                arguments("1", "10", -1),
                arguments("1", "100", -1),
                arguments("11", "10", 1),
                arguments("10", "10", 0),
                arguments("2", "1", 1),
                arguments("2", "10", -1),
                arguments("02", "10", -1),
                arguments("02", "1", 1),
                arguments("a", "b", -1),
                arguments("a", "aa", -1),
                arguments("c", "a", 1),
                arguments("d", "d", 0),
                arguments("qw42", "ab42", 1),
                arguments("qw42", "ab43", 1),
                arguments("ab42", "ab43", -1),
                arguments("ab420", "ab43", 1),
                arguments("ab42cd", "ab42", 1),
                arguments("ab42cd", "ab42cd", 0),
                arguments("ab42cd2", "ab42cd10", -1),
                arguments("cpu 2", "cpu10", -1),
                arguments("cpu 1core10", "cpu1 core 2", 1),
                arguments(" cpu42", "  cpu     42   ", 0),
                arguments(" cpu42", "  cpu     120   ", -1),
                arguments("cpu 2", "cpu2core3", -1),
                arguments("cpu 1 core 10 thread1", "cpu1core2thread2", 1),
                arguments("", "cpu", -1),
                arguments("all", "0", 1)
            )
        }
    }
}
