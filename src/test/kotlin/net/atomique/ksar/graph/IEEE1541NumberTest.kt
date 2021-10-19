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

class IEEE1541NumberTest {
    @ParameterizedTest
    @MethodSource("testValues")
    fun testFormat(testNumber: Double, expectedFactor1: String, expectedFactor1024: String) {
        assertAll(
            {
                assertEquals(
                    expectedFactor1,
                    IEEE1541Number(1).format(testNumber)
                ) { "factor=1; input number: $testNumber" }
            },
            {
                assertEquals(
                    expectedFactor1024,
                    IEEE1541Number(1024).format(testNumber)
                ) { "factor=1024; input number: $testNumber" }
            }
        )
    }

    companion object {
        private val fmt = DecimalFormat("#,##0.0")

        private infix fun Double.fmt(unit: String) =
            fmt.format(this) + (if (unit.isBlank()) "" else " ") + unit

        @JvmStatic
        fun testValues() = Stream.of(
            arguments(791.5, 791.5 fmt "", 791.5 fmt "Ki"),
            arguments(9462.04, 9.2 fmt "Ki", 9.2 fmt "Mi"),
            arguments(25414.88, 24.8 fmt "Ki", 24.8 fmt "Mi"),
            arguments(725414.88, 708.4 fmt "Ki", 708.4 fmt "Mi"),
            arguments(2725414.88, 2.6 fmt "Mi", 2.6 fmt "Gi"),
            arguments(27254140.88, 26.0 fmt "Mi", 26.0 fmt "Gi"),
            arguments(272541400.88, 259.9 fmt "Mi", 259.9 fmt "Gi"),
            arguments(2725414000.88, 2.5 fmt "Gi", 2599.2 fmt "Gi"),
            arguments(27254140000.88, 25.4 fmt "Gi", 25991.6 fmt "Gi"),
            arguments(272541400000.88, 253.8 fmt "Gi", 259915.7 fmt "Gi")
        )
    }
}
