/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.parser

import net.atomique.ksar.Config
import net.atomique.ksar.kSar
import org.junit.jupiter.api.Assertions.assertEquals
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
        val ksar = kSar()
        Config.setLinuxDateFormat("Automatic Detection")
        val sut = Linux().apply {
            init(ksar, header)
            parse(sarString, columns)
        }
        assertEquals(expectedDate, sut.startOfGraph) { "header: $header, sar string: $sarString" }
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
