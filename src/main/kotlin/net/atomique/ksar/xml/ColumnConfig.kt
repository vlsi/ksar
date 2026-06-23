/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.xml

import org.slf4j.LoggerFactory
import java.awt.Color
import java.lang.IllegalArgumentException

class ColumnConfig(val data_title: String) {
    var type = 0
        private set
    var dataColor: Color? = null
        private set

    companion object {
        private val log = LoggerFactory.getLogger(ColumnConfig::class.java)
    }

    fun setDataColor(dataColorString: String) {
        val colorIndices = dataColorString.split(",")
        if (colorIndices.size == 3) {
            try {
                dataColor = Color(colorIndices[0].toInt(), colorIndices[1].toInt(), colorIndices[2].toInt())
            } catch (iae: IllegalArgumentException) {
                log.warn("Column color error for {} - <{}>", data_title, dataColorString, iae)
            }
        } else {
            log.warn("Wrong Color definition for {} - <{}>", data_title, dataColorString)
        }
    }

    fun setType(s: String) {
        if ("gauge" == s) {
            type = 1
        }
        if ("counter" == s) {
            type = 2
        }
    }

    fun isValid(): Boolean = dataColor != null
}
