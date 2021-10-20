/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.ui

import java.util.Comparator
import java.util.regex.Pattern

/**
 * Compares strings in "human natural order".
 * E.g. `"cpu 2" < "cpu 10"`, and `"cpu 1 core 10 thread1" >  "cpu1core2thread2"`
 */
object NaturalComparator : Comparator<String> {
    @JvmStatic
    val NULLS_FIRST = Comparator.nullsFirst(this)

    private val WORD_PATTERN = Pattern.compile("\\s*+([^0-9\\s]|\\d+)")

    override fun compare(a: String?, b: String?): Int {
        if (a == null || b == null) {
            return 0 // nulls should be handled in other comparator
        }
        val ma = WORD_PATTERN.matcher(a)
        val mb = WORD_PATTERN.matcher(b)
        while (ma.find() && mb.find()) {
            val u = ma.group(1)
            val v = mb.group(1)
            val isDigit = Character.isDigit(u[0])
            if (!isDigit || !Character.isDigit(v[0])) {
                val res = u.compareTo(v)
                if (res != 0) {
                    return res
                }
                continue
            }
            val aLong = u.toLong()
            val bLong = v.toLong()
            if (aLong != bLong) {
                return aLong.compareTo(bLong)
            }
        }
        return if (ma.find()) 1 else if (mb.find()) -1 else 0
    }
}
