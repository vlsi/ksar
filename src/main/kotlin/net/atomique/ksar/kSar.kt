/*
 * Copyright 2008 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar

import net.atomique.ksar.GlobalOptions.cLfilename
import net.atomique.ksar.GlobalOptions.getParser
import net.atomique.ksar.ui.DataView
import net.atomique.ksar.ui.SortedTreeNode
import net.atomique.ksar.ui.TreeNodeInfo
import org.slf4j.LoggerFactory
import java.beans.PropertyVetoException
import java.io.BufferedReader
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import javax.swing.JDesktopPane

class kSar {
    var graphtree = SortedTreeNode("kSar")
    val dataView = DataView(this)
    private var linesParsed: Long = 0
    private var reloadAction: String? = "Empty"
    private var launchedAction: Thread? = null
    private var actionInterrupted = false
    lateinit var myparser: OSParser
    var isParsing = false
        private set
    private var pageToPrint = 0

    companion object {
        private val log = LoggerFactory.getLogger(kSar::class.java)
    }

    constructor(desktopPane: JDesktopPane) {
        dataView.toFront()
        dataView.isVisible = true
        dataView.title = "Empty"
        desktopPane.add(dataView)
        try {
            val num = desktopPane.allFrames.size
            if (num != 1) {
                dataView.reshape(5 * num, 5 * num, 800, 600)
            } else {
                dataView.reshape(0, 0, 800, 600)
            }
            dataView.isSelected = true
        } catch (vetoe: PropertyVetoException) {
            log.error("PropertyVetoException", vetoe)
        }
        if (cLfilename != null) {
            do_fileread(cLfilename)
        }
    }

    constructor() {}

    fun do_fileread(filename: String?) {
        launchedAction = if (filename == null) {
            FileRead(this)
        } else {
            FileRead(this, filename)
        }
        reloadAction = (launchedAction as FileRead).get_action()
        do_action()
    }

    fun do_localcommand(cmd: String?) {
        launchedAction = if (cmd == null) {
            LocalCommand(this)
        } else {
            LocalCommand(this, cmd)
        }
        reloadAction = (launchedAction as LocalCommand).get_action()
        do_action()
    }

    fun do_sshread(cmd: String?) {
        launchedAction = if (cmd == null) {
            SSHCommand(this)
            // mysar.reload_command=t.get_command();
        } else {
            SSHCommand(this, cmd)
        }
        reloadAction = (launchedAction as SSHCommand).get_action()
        do_action()
    }

    private fun do_action() {
        if (reloadAction == null) {
            log.info("action is null")
            return
        }
        if (launchedAction != null) {
            dataView.notifyrun(true)
            launchedAction!!.start()
        }
    }

    fun parse(br: BufferedReader): Int {
        var currentLine: String = ""
        var parserReturn: Int
        val parsingStart = System.currentTimeMillis()
        try {
            while (br.readLine()?.also { currentLine = it } != null && !actionInterrupted) {
                isParsing = true
                linesParsed++
                if (currentLine.isEmpty()) {
                    continue
                }
                val columns = currentLine.split(Regex("\\s+")).toTypedArray()
                if (columns.isEmpty()) {
                    continue
                }

                // log.debug("Header Line : {}", current_line);
                val firstColumn = columns[0]
                try {
                    if (!::myparser.isInitialized) {
                        val parserClass = getParser(firstColumn)
                        myparser = parserClass.getConstructor(kSar::class.java, String::class.java)
                            .newInstance(this, currentLine)
                        continue
                    } else {
                        if (myparser.parserName == columns[0]) {
                            myparser.parse_header(currentLine)
                            continue
                        }
                    }
                } catch (ex: InstantiationException) {
                    log.error("Parser Exception", ex)
                } catch (ex: IllegalAccessException) {
                    log.error("Parser Exception", ex)
                } catch (ex: NoSuchMethodException) {
                    log.error("Parser Exception", ex)
                } catch (ex: InvocationTargetException) {
                    log.error("Parser Exception", ex)
                }
                if (!::myparser.isInitialized) {
                    log.error("unknown parser")
                    isParsing = false
                    return -1
                }
                parserReturn = myparser.parse(currentLine, columns)
                if (parserReturn == 1) {
                    log.trace("### {}", currentLine)
                }
                if (parserReturn < 0) {
                    log.trace("ERR {}", currentLine)
                }
                myparser.updateUITitle()
            }
        } catch (ex: IOException) {
            log.error("IO Exception", ex)
            isParsing = false
        }
        dataView.treehome()
        dataView.notifyrun(false)
        dataView.setHasData(true)
        val parsingEnd = System.currentTimeMillis()
        log.trace("time to parse: {} ms", parsingEnd - parsingStart)
        log.trace("lines parsed: {}", linesParsed)
        log.trace("number of datesamples: {}", myparser.dateSamples.size)
        isParsing = false
        return -1
    }

    fun cleared() {
        aborted()
    }

    private fun aborted() {
        log.trace("reset menu")
        dataView.notifyrun(false)
    }

    fun interrupt_parsing() {
        if (isParsing) {
            actionInterrupted = true
        }
    }

    fun add2tree(parent: SortedTreeNode?, newNode: SortedTreeNode?) {
        dataView.add2tree(parent, newNode)
    }

    fun get_page_to_print(): Int {
        pageToPrint = 0
        countPrintSelected(graphtree)
        return pageToPrint
    }

    private fun countPrintSelected(node: SortedTreeNode) {
        val num = node.childCount
        if (num > 0) {
            for (i in 0 until num) {
                val l = node.getChildAt(i) as SortedTreeNode
                countPrintSelected(l)
            }
        } else {
            val obj1 = node.userObject
            if (obj1 is TreeNodeInfo) {
                val nodeObj = obj1.node_object
                if (nodeObj.isPrintSelected) {
                    pageToPrint++
                }
            }
        }
    }
}
