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

class HpuxHeaderTest {
    @ParameterizedTest
    @MethodSource("testValues")
    fun test(header: String, sarString: String, expectedDate: LocalDateTime) {
        val columns = sarString.split(Regex("\\s+")).toTypedArray()
        val sut = HPUX(kSar(), header).apply {
            parse(sarString, columns)
        }
        assertEquals(expectedDate, sut.startOfGraph) { "header: $header, sar string: $sarString" }
    }

    companion object {
        @JvmStatic
        fun testValues(): Stream<Arguments> {
            val str = "2018-03-21 00:05:01"
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val dateTime = LocalDateTime.parse(str, formatter)
            return Stream.of(
                arguments(
                    "HP-UX hostname.example.com B.11.31 U ia64    03/21/18",
                    "00:05:01      19       8      29      45",
                    dateTime
                )
            )
        }
    }
}
