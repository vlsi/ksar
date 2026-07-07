/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.xml

import org.jfree.chart.axis.NumberAxis
import net.atomique.ksar.graph.IEEE1541Number
import net.atomique.ksar.graph.ISNumber
import org.jfree.data.Range
import org.slf4j.LoggerFactory

class PlotStackConfig(val title: String) {
    private var factor = 0.0
    private var base = 0
    private var range: Range? = null
    var size = 1
    var header: List<String>? = null
        private set
    var headerStr: String? = null
        private set

    companion object {
        private val log = LoggerFactory.getLogger(PlotStackConfig::class.java)
    }

    fun setHeaderStr(s: String) {
        header = s.split(Regex("\\s+"))
        headerStr = s
    }

    fun setSize(size: String?) {
        this.size = Integer.valueOf(size)
    }

    val axis: NumberAxis
        get() {
            val tmp = NumberAxis(title)
            if (base == 1024) {
                tmp.numberFormatOverride = IEEE1541Number(factor.toInt())
            } else if (base == 1000) {
                tmp.numberFormatOverride = ISNumber(factor.toInt())
            } else if (base != 0) {
                log.error("base value is not handled")
            }
            if (range != null) {
                tmp.range = range
            }
            return tmp
        }

    fun setBase(s: String?) {
        if (s == null) {
            return
        }
        base = Integer.parseUnsignedInt(s)
    }

    fun setFactor(s: String) {
        factor = s.toDouble()
    }

    fun setRange(s: String) {
        val t = s.split(",")
        if (t.size == 2) {
            val min = t[0].toDouble()
            val max = t[1].toDouble()
            range = Range(min, max)
        }
    }
}
