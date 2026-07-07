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

    private val WORD_PATTERN = Pattern.compile("\\s*+([^0-9\\s]++|\\d++)")

    override fun compare(a: String?, b: String?): Int {
        if (a == null || b == null) {
            return 0 // nulls should be handled in other comparator
        }
        val ma = WORD_PATTERN.matcher(a)
        val mb = WORD_PATTERN.matcher(b)
        while (true) {
            val findA = ma.find()
            val findB = mb.find()
            if (findA && !findB) {
                return 1
            }
            if (!findA) {
                return if (findB) -1 else 0
            }
            val u = ma.group(1)
            val v = mb.group(1)
            if (Character.isDigit(u[0]) && Character.isDigit(v[0])) {
                val res = u.length.compareTo(v.length)
                if (res != 0) {
                    // The shorter the length the smaller the number
                    return res
                }
            }
            // Either both are numeric of equal length (see above if)
            // or they are non-numeric, then we compare as strings
            // or one of them is numeric and another is not, then we compare as strings anyway
            val res = u.compareTo(v)
            if (res != 0) {
                return res
            }
        }
    }
}
