/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.parser

import net.atomique.ksar.kSar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Stream

class SolarisHeaderTest {
    @ParameterizedTest
    @MethodSource("testValues")
    fun test(header: String, sarString: String, expectedDate: LocalDateTime) {
        val columns = sarString.split(Regex("\\s+")).toTypedArray()
        val ksar = kSar()
        val sut = SunOS().apply {
            init(ksar, header)
            parse(sarString, columns)
        }
        assertEquals(expectedDate, sut.startOfGraph) { "header: $header, sar string: $sarString" }
    }

    companion object {
        @JvmStatic
        fun testValues(): Stream<Arguments> {
            val str = "05/31/2018 00:05:01"
            val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
            val dateTime = LocalDateTime.parse(str, formatter)
            return Stream.of(
                arguments(
                    "SunOS hostname.example.com 5.11 11.3 sun4v    05/31/2018",
                    "00:05:01    %usr    %sys    %wio   %idle",
                    dateTime
                )
            )
        }
    }
}
