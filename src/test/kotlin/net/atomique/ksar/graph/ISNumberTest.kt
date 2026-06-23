/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.graph

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.text.DecimalFormat
import java.util.stream.Stream

class ISNumberTest {
    @ParameterizedTest
    @MethodSource("testValues")
    fun testFormat(testNumber: Double, expectedFactor1: String, expectedFactor1000: String) {
        assertAll(
            {
                assertEquals(
                    expectedFactor1,
                    ISNumber(1).format(testNumber)
                ) { "factor=1; input number: $testNumber" }
            },
            {
                assertEquals(
                    expectedFactor1000,
                    ISNumber(1000).format(testNumber)
                ) { "factor=1000; input number: $testNumber" }
            }
        )
    }

    companion object {
        private val fmt = DecimalFormat("#,##0.0")

        private infix fun Double.fmt(unit: String) =
            fmt.format(this) + (if (unit.isBlank()) "" else " ") + unit

        @JvmStatic
        fun testValues() = Stream.of(
            arguments(791.5, 791.5 fmt "", 791.5 fmt "K"),
            arguments(9462.04, 9.5 fmt "K", 9.5 fmt "M"),
            arguments(25414.88, 25.4 fmt "K", 25.4 fmt "M"),
            arguments(725414.88, 725.4 fmt "K", 725.4 fmt "M"),
            arguments(2725414.88, 2.7 fmt "M", 2.7 fmt "G"),
            arguments(27254140.88, 27.3 fmt "M", 27.3 fmt "G"),
            arguments(272541400.88, 272.5 fmt "M", 272.5 fmt "G"),
            arguments(2725414000.88, 2.7 fmt "G", 2725.4 fmt "G"),
            arguments(27254140000.88, 27.3 fmt "G", 27254.1 fmt "G"),
            arguments(272541400000.88, 272.5 fmt "G", 272541.4 fmt "G")
        )
    }
}
