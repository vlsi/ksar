/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.xml

class GraphConfig(val name: String, val title: String, val type: String) {
    val plotlist = mutableMapOf<String, PlotStackConfig>()
    val stacklist = mutableMapOf<String, PlotStackConfig>()

    fun addPlot(s: PlotStackConfig) {
        plotlist[s.title] = s
    }

    fun addStack(s: PlotStackConfig) {
        stacklist[s.title] = s
    }
}
