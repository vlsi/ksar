/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.graph

import net.atomique.ksar.kSar
import net.atomique.ksar.xml.GraphConfig
import java.time.LocalDateTime
import org.jfree.data.time.Second
import net.atomique.ksar.ui.TreeNodeInfo
import net.atomique.ksar.ui.SortedTreeNode
import javax.swing.JPanel
import java.awt.GridLayout
import javax.swing.border.TitledBorder
import javax.swing.BoxLayout
import java.util.SortedMap
import java.util.TreeMap
import net.atomique.ksar.ui.NaturalComparator
import net.atomique.ksar.ui.ParentNodeInfo
import kotlin.math.floor

class List(
    private var mysar: kSar,
    private val graphConfig: GraphConfig,
    val title: String,
    private val headerStr: String,
    private val firstDataColumn: Int
) {
    private val parentTreeNode = SortedTreeNode(ParentNodeInfo(title, this))
    private val nodeHashList: SortedMap<String, Graph> = TreeMap(NaturalComparator.NULLS_FIRST)

    init {
        mysar.add2tree(mysar.graphtree, parentTreeNode)
    }

    fun parse_line(ldt: LocalDateTime, s: String): Int {
        val now = Second(
            ldt.second,
            ldt.minute,
            ldt.hour,
            ldt.dayOfMonth,
            ldt.monthValue,
            ldt.year
        )
        return parse_line(now, s)
    }

    fun parse_line(now: Second, s: String): Int {
        val cols = s.split(Regex("\\s+"))
        val tmp: Graph
        if (!nodeHashList.containsKey(cols[firstDataColumn])) {
            tmp = Graph(
                mysar, graphConfig, title + " " + cols[firstDataColumn], headerStr, firstDataColumn + 1,
                null
            )
            nodeHashList[cols[firstDataColumn]] = tmp
            val treeNodeInfo = TreeNodeInfo(cols[firstDataColumn], tmp)
            val sortedTreeNode = SortedTreeNode(treeNodeInfo)
            mysar.add2tree(parentTreeNode, sortedTreeNode)
        } else {
            tmp = nodeHashList.getValue(cols[firstDataColumn])
        }
        return tmp.parse_line(now, s)
    }

    fun run(): JPanel {
        val graphNumber = nodeHashList.size
        var linenum = floor((graphNumber / 2).toDouble()).toInt()
        if (graphNumber % 2 != 0) {
            linenum++
        }
        val tmppanel = JPanel()
        tmppanel.layout = GridLayout(linenum, 2)
        for (graph in nodeHashList.values) {
            tmppanel.add(graph.getChartPanel())
        }
        return tmppanel
    }

    val isPrintSelected: Boolean
        get() {
            var leaftoprint = false
            for (graph in nodeHashList.values) {
                if (graph.isPrintSelected) {
                    leaftoprint = true
                    break
                }
            }
            return leaftoprint
        }

    fun getprintform(): JPanel {
        val panel = JPanel()
        panel.border = TitledBorder(title)
        panel.layout = BoxLayout(panel, BoxLayout.PAGE_AXIS)
        return panel
    }
}
