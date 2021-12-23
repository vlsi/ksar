/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.xml

import org.slf4j.LoggerFactory
import java.util.*

class CnxHistory(val link: String) {
    var username: String? = null
    var hostname: String? = null
    val commandList = TreeSet<String>()
    var port: String? = null

    init {
        val s = link.split("@", limit = 2)
        if (s.size == 2) {
            username = s[0]
            val t = s[1].split(":")
            if (t.size == 2) {
                hostname = t[0]
                port = t[1]
            } else {
                hostname = s[1]
                port = "22"
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CnxHistory::class.java)
    }

    fun addCommand(s: String) {
        commandList.add(s)
    }

    val portInt: Int
        get() = port!!.toInt()

    val isValid: Boolean
        get() = username != null && hostname != null && commandList.isNotEmpty()

    fun dump() {
        log.debug("$username@$hostname:$commandList")
    }

    fun save(): String {
        val tmp = StringBuilder()
        if ("22" == port) {
            tmp.append("\t\t<cnx link=\"$username@$hostname\">\n")
        } else {
            tmp.append("\t\t<cnx link=\"$username@$hostname:$port\">\n")
        }
        for (cmd in commandList) {
            tmp.append("\t\t\t<command>").append(cmd).append("</command>\n")
        }
        tmp.append("\t\t</cnx>\n")
        return tmp.toString()
    }
}
