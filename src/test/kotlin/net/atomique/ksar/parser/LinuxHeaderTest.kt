/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.parser

import net.atomique.ksar.Config
import net.atomique.ksar.kSar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Stream

class LinuxHeaderTest {
    @ParameterizedTest
    @MethodSource("testValues")
    fun test(header: String, sarString: String, expectedDate: LocalDateTime) {
        val columns = sarString.split(Regex("\\s+")).toTypedArray()
        Config.linuxDateFormat = "Automatic Detection"
        val sut = Linux(kSar(), header).apply {
            parse(sarString, columns)
        }
        assertEquals(expectedDate, sut.startOfGraph) { "header: $header, sar string: $sarString" }
    }

    @Test
    fun reDetectsDateFormatForEachHeader() {
        Config.linuxDateFormat = "Automatic Detection"
        val stat =
            "09:10:01          6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00"

        // 03/04/16 is ambiguous; the latest non-future reading is dd/MM/yy -> 2016-04-03.
        val sut = Linux(kSar(), "Linux 3.10.0-327.el7.x86_64 (host)  03/04/16  _x86_64_  (48 CPU)")
        sut.parse(stat, stat.split(Regex("\\s+")).toTypedArray())

        // 12/31/23 only parses as MM/dd/yy -> 2023-12-31. A formatter cached from the first
        // header (dd/MM/yy) would reject it, so the format must be detected again per header.
        sut.parse_header("Linux 3.10.0-327.el7.x86_64 (host)  12/31/23  _x86_64_  (48 CPU)")
        sut.parse(stat, stat.split(Regex("\\s+")).toTypedArray())

        assertEquals(LocalDateTime.of(2016, 4, 3, 9, 10, 1), sut.startOfGraph)
        assertEquals(LocalDateTime.of(2023, 12, 31, 9, 10, 1), sut.endOfGraph)
    }

    companion object {
        @JvmStatic
        fun testValues(): Stream<Arguments> {
            val str = "2016-03-28 09:10:01"
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val dateTime = LocalDateTime.parse(str, formatter)
            return Stream.of(
                arguments(
                    "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  03/28/16  _x86_64_  (48 CPU)",
                    "09:10:01          6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
                    dateTime
                ),
                arguments(
                    "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  03/28/2016  _x86_64_  (48 CPU)",
                    "09:10:01          6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
                    dateTime
                ),
                arguments(
                    "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  20160328  _x86_64_  (48 CPU)",
                    "09:10:01          6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
                    dateTime
                ),
                arguments(
                    "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  2016-03-28  _x86_64_  (48 CPU)",
                    "09:10:01          6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
                    dateTime
                ),
                arguments(
                    "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  03/28/16  _x86_64_  (48 CPU)",
                    "09:10:01    AM      6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
                    dateTime
                ),
                arguments(
                    "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  03/28/2016  _x86_64_  (48 CPU)",
                    "09:10:01    AM      6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
                    dateTime
                ),
                arguments(
                    "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  20160328  _x86_64_  (48 CPU)",
                    "09:10:01     AM     6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
                    dateTime
                ),
                arguments(
                    "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  2016-03-28  _x86_64_  (48 CPU)",
                    "09:10:01    AM      6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
                    dateTime
                )
            )
        }
    }
}
