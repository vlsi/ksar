/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.graph

import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.NumberFormat
import java.text.ParsePosition

class ISNumber(factor: Int) : NumberFormat() {
    private val kilo = factor

    companion object {
        private const val IS_kilo = 1000.0
        private const val IS_mega = 1000000.0
        private const val IS_giga = 1000000000.0
    }

    override fun parse(source: String, parsePosition: ParsePosition): Number? = null

    override fun format(number: Long, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
        return format((number * kilo).toDouble(), toAppendTo, pos)
    }

    override fun format(number: Double, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
        val formatter = DecimalFormat("#,##0.0")
        if (kilo == 0) {
            return toAppendTo.append(formatter.format(number))
        }
        if (number * kilo < IS_kilo) {
            return toAppendTo.append(formatter.format(number))
        }
        if (number * kilo < IS_mega) {
            return toAppendTo.append(formatter.format(number * kilo / IS_kilo)).append(" K")
        }
        if (number * kilo < IS_giga) {
            return toAppendTo.append(formatter.format(number * kilo / IS_mega)).append(" M")
        }
        return toAppendTo.append(formatter.format(number * kilo / IS_giga)).append(" G")
    }
}
