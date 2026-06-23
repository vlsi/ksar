/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.graph

import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.NumberFormat
import java.text.ParsePosition

class IEEE1541Number(factor: Int) : NumberFormat() {
    private val kilo = factor

    companion object {
        private const val IEC_kibi = 1024.0
        private const val IEC_mebi = 1048576.0
        private const val IEC_gibi = 1073741824.0
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
        if (number * kilo < IEC_kibi) {
            return toAppendTo.append(formatter.format(number))
        }
        if (number * kilo < IEC_mebi) {
            return toAppendTo.append(formatter.format(number * kilo / IEC_kibi)).append(" Ki")
        }
        if (number * kilo < IEC_gibi) {
            return toAppendTo.append(formatter.format(number * kilo / IEC_mebi)).append(" Mi")
        }
        return toAppendTo.append(formatter.format(number * kilo / IEC_gibi)).append(" Gi")
    }
}
