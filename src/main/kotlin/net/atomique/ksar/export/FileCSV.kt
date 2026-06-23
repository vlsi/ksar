/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.export

import net.atomique.ksar.kSar
import java.lang.Runnable
import javax.swing.JProgressBar
import javax.swing.JDialog
import java.time.format.DateTimeFormatter
import org.jfree.data.time.Second
import java.io.IOException
import net.atomique.ksar.ui.SortedTreeNode
import net.atomique.ksar.ui.TreeNodeInfo
import org.jfree.data.time.RegularTimePeriod
import org.slf4j.LoggerFactory
import java.lang.StringBuilder
import java.nio.file.Paths
import kotlin.io.path.writeText

class FileCSV(private val csvfilename: String, private val mysar: kSar) : Runnable {
    private val tmpcsv = StringBuilder()
    private var progressInfo = 0
    private var progressBar: JProgressBar? = null
    private var dialog: JDialog? = null

    companion object {
        private val log = LoggerFactory.getLogger(FileCSV::class.java)
    }

    constructor(filename: String, hissar: kSar, g: JProgressBar?, d: JDialog?) : this(filename, hissar) {
        progressBar = g
        dialog = d
    }

    override fun run() {
        // print header
        tmpcsv.append("Date;")
        export_treenode_header(mysar.graphtree)
        tmpcsv.append("\n")
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss")
        for (tmpLDT in mysar.myparser.dateSamples) {
            val text = tmpLDT.format(formatter)
            tmpcsv.append(text).append(";")
            val tmp = Second(
                tmpLDT.second,
                tmpLDT.minute,
                tmpLDT.hour,
                tmpLDT.dayOfMonth,
                tmpLDT.monthValue,
                tmpLDT.year
            )
            exportTreeNodeData(mysar.graphtree, tmp)
            tmpcsv.append("\n")
        }
        try {
            Paths.get(csvfilename).writeText(tmpcsv)
        } catch (ex: IOException) {
            log.error("CSV IO Exception", ex)
        }
        dialog?.dispose()
    }

    private fun export_treenode_header(node: SortedTreeNode) {
        val num = node.childCount
        if (num > 0) {
            /*Object obj1 = node.getUserObject();
      if (obj1 instanceof ParentNodeInfo) {
        ParentNodeInfo tmpnode = (ParentNodeInfo) obj1;
        List nodeobj = tmpnode.getNode_object();
      }*/
            for (i in 0 until num) {
                val l = node.getChildAt(i) as SortedTreeNode
                export_treenode_header(l)
            }
        } else {
            val obj1 = node.userObject
            if (obj1 is TreeNodeInfo) {
                val nodeObj = obj1.node_object
                if (nodeObj.doPrint()) {
                    tmpcsv.append(nodeObj.csvHeader)
                }
            }
        }
    }

    private fun exportTreeNodeData(node: SortedTreeNode, time: RegularTimePeriod) {
        val num = node.childCount
        if (num > 0) {
            /*Object obj1 = node.getUserObject();
        if (obj1 instanceof ParentNodeInfo) {
        ParentNodeInfo tmpnode = (ParentNodeInfo) obj1;
        List nodeobj = tmpnode.getNode_object();
      }*/
            for (i in 0 until num) {
                val l = node.getChildAt(i) as SortedTreeNode
                exportTreeNodeData(l, time)
            }
        } else {
            val obj1 = node.userObject
            if (obj1 is TreeNodeInfo) {
                val nodeObj = obj1.node_object
                if (nodeObj.doPrint()) {
                    tmpcsv.append(nodeObj.getCsvLine(time))
                    updateUi()
                }
            }
        }
    }

    private fun updateUi() {
        progressBar?.let {
            it.value = ++progressInfo
            it.repaint()
        }
    }
}
