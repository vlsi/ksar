/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.xml

import java.lang.NumberFormatException

class HostInfo(var hostname: String) {
    var alias: String? = null
    var description: String? = null
    var memBlockSize = 1

    fun setMemBlockSize(value: String) {
        try {
            memBlockSize = value.toInt()
        } catch (nfe: NumberFormatException) {
        }
    }

    fun save() =
        """
        <host name="$hostname">
            <alias>$alias</alias>
            <description>$description</description>
            <memblocksize>$memBlockSize</memblocksize>
        </host>
        """.trimIndent()
}
