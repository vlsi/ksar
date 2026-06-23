/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.xml

import java.util.Arrays
import java.util.HashMap
import java.util.function.Consumer

class OSConfig(var osName: String) {
    val statHash = HashMap<String, StatConfig>()
    val graphHash = HashMap<String, GraphConfig>()
    private var mappingStatHeaderName: HashMap<String?, String>? = null

    fun addStat(s: StatConfig) {
        statHash[s.statName] = s
    }

    fun addGraph(s: GraphConfig) {
        graphHash[s.name] = s
    }

    fun getStat(columns: Array<String>, firstdatacolumn: Int): String? {
        // this is called for each line of source file
        val s1 = Arrays.copyOfRange(columns, firstdatacolumn, columns.size)
        val header = java.lang.String.join(" ", *s1)

        // cache Mapping of HeaderStr to StatName - get StatHash more efficiently
        createCacheForMappingOfHeaderStr2StatName()
        val statName = mappingStatHeaderName!![header]
        return if (statName != null) {
            statHash[statName]!!.graphName
        } else {
            null
        }
    }

    private fun createCacheForMappingOfHeaderStr2StatName() {
        // do this only once - all Stats are known - create reverse maaping
        if (mappingStatHeaderName == null) {
            mappingStatHeaderName = HashMap()
            statHash.forEach { (k: String?, v: StatConfig) ->
                val statName = v.statName
                val graphHeader = v.headerStr
                mappingStatHeaderName!![graphHeader] = statName
            }
        }
    }

    fun getStat(statName: String): StatConfig? {
        if (statHash.isEmpty()) {
            return null
        }
        val result = arrayOf<StatConfig?>(null)
        statHash.keys.forEach(
            Consumer { item: String? ->
                val tmp = statHash[item]
                if (tmp!!.graphName == statName) {
                    result[0] = tmp
                }
            }
        )
        return result[0]
    }

    fun getGraphConfig(s: String): GraphConfig? {
        return if (graphHash.isEmpty()) {
            null
        } else graphHash[s]
    }
}
