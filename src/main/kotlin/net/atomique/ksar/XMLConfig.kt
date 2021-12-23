/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar

import net.atomique.ksar.GlobalOptions.colorlist
import net.atomique.ksar.GlobalOptions.historyList
import net.atomique.ksar.GlobalOptions.hostInfoList
import net.atomique.ksar.GlobalOptions.oSlist
import net.atomique.ksar.xml.*
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import java.util.function.Consumer
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

class XMLConfig : DefaultHandler() {
    private var beenparse = false
    private lateinit var tempval: String
    private var in_color = false
    private var in_colors = false
    private var in_OS = false
    private var in_history = false
    private var in_cnx = false
    private var in_hostinfo = false
    private var in_host = false
    private var currentColor: ColumnConfig? = null
    private var currentOS: OSConfig? = null
    private var currentStat: StatConfig? = null
    private var currentGraph: GraphConfig? = null
    private var currentPlot: PlotStackConfig? = null
    private var currentStack: PlotStackConfig? = null
    private var currentCnx: CnxHistory? = null
    private var currentHost: HostInfo? = null

    companion object {
        private const val KSAR_DTD_PREFIX = "-//NET/ATOMIQUE/KSAR/"
        private val log = LoggerFactory.getLogger(XMLConfig::class.java)
    }

    @Throws(IOException::class)
    override fun resolveEntity(publicId: String?, systemId: String): InputSource? {
        if (publicId == null || !publicId.startsWith(KSAR_DTD_PREFIX)) {
            return null
        }
        var dtdFile = publicId.substring(KSAR_DTD_PREFIX.length - 1)
        dtdFile = dtdFile.lowercase(Locale.getDefault())
        val inputStream = javaClass.getResourceAsStream(dtdFile)
            ?: throw FileNotFoundException("File $publicId is not found in kSar resources")
        return InputSource(inputStream)
    }

    fun loadFromResources(fileName: String) {
        try {
            javaClass.getResourceAsStream(fileName).use { `is` ->
                if (`is` == null) {
                    throw FileNotFoundException("File $fileName is not found in kSar resources")
                }
                val source = InputSource(`is`)
                source.publicId = KSAR_DTD_PREFIX + fileName
                loadConfig(source)
            }
        } catch (e: IOException) {
            log.warn("XML error while parsing $fileName", e)
        }
    }

    fun loadConfig(file: File) {
        val source = InputSource(file.toURI().toASCIIString())
        loadConfig(source)
    }

    private fun loadConfig(source: InputSource) {
        val fabric: SAXParserFactory
        val parser: SAXParser
        val id = source.systemId + " (" + source.publicId + ")"
        try {
            fabric = SAXParserFactory.newInstance()
            parser = fabric.newSAXParser()
            parser.parse(source, this)
        } catch (ex: ParserConfigurationException) {
            log.warn("XML error while parsing $id", ex)
        } catch (ex: SAXException) {
            log.warn("XML error while parsing $id", ex)
        } catch (ioe: IOException) {
            val msg = "IO exception while parsing $id"
            log.error(msg, ioe)
            throw IllegalArgumentException(msg, ioe)
        }
    }

    fun dump_XML() {
        oSlist.keys.forEach(
            Consumer { item: String? ->
                val tmp = oSlist[item]
                log.trace("-OS-{}", tmp!!.osName)
                tmp.statHash.keys.forEach(
                    Consumer { stat: String? ->
                        val tmp2 = tmp.statHash[stat]
                        log.trace(
                            "--STAT-- " +
                                tmp2!!.statName + "=> " +
                                tmp2.graphName + " " +
                                tmp2.headerStr
                        )
                    }
                )
                tmp.graphHash.keys.forEach(
                    Consumer { graph: String? ->
                        val tmp3 = tmp.graphHash[graph]
                        log.trace(
                            "---GRAPH--- " +
                                tmp3!!.name + "=> " +
                                tmp3.title
                        )
                        tmp3.plotlist.keys.forEach(
                            Consumer { plot: String? ->
                                val tmp4 = tmp3.plotlist[plot]
                                log.trace(
                                    "----PLOT---- " +
                                        tmp4!!.title + "=> " +
                                        tmp4.headerStr
                                )
                            }
                        )
                    }
                )
            }
        )
    }

    @Throws(SAXException::class)
    override fun characters(ch: CharArray, start: Int, length: Int) {
        tempval = String(ch, start, length)
    }

    @Throws(SAXException::class)
    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {

        /*
    if ("ConfiG".equals(qName)) {
      // config found
    }
    */
        if ("colors" == qName) {
            in_colors = true
        }
        if ("OS" == qName) {
            in_OS = true
        }
        if ("History" == qName) {
            in_history = true
        }
        if ("HostInfo" == qName) {
            in_hostinfo = true
        }

        // COLORS
        if (in_colors) {
            if ("itemcolor" == qName) {
                currentColor = ColumnConfig(attributes.getValue("name"))
                in_color = true
            }
        }

        // history
        if (in_history) {
            if ("cnx" == qName) {
                currentCnx = CnxHistory(attributes.getValue("link"))
                in_cnx = true
            }
        }
        // hostinfo
        if (in_hostinfo) {
            if ("host" == qName) {
                currentHost = HostInfo(attributes.getValue("name"))
                in_host = true
            }
        }

        // OS
        if (in_OS) {
            if ("OSType" == qName) {
                currentOS = oSlist[attributes.getValue("name")]
                if (currentOS == null) {
                    currentOS = OSConfig(attributes.getValue("name")).also {
                        oSlist[it.osName] = it
                    }
                    oSlist[currentOS!!.osName] = currentOS!!
                }
            }
            if (currentOS != null) {
                if ("Stat" == qName) {
                    currentStat = StatConfig(attributes.getValue("name")).also {
                        currentOS?.addStat(it)
                    }
                }
                if ("Graph" == qName) {
                    currentGraph = GraphConfig(
                        attributes.getValue("name"), attributes.getValue("Title"),
                        attributes.getValue("type")
                    ).also {
                        currentOS?.addGraph(it)
                    }
                }
                if (currentGraph != null) {
                    if ("Plot" == qName) {
                        currentPlot = PlotStackConfig(attributes.getValue("Title")).also { plot ->
                            attributes.getValue("size")?.let {
                                plot.setSize(it)
                            }
                            currentGraph?.addPlot(plot)
                        }
                    }
                    if ("Stack" == qName) {
                        currentStack = PlotStackConfig(attributes.getValue("Title")).also { stack ->
                            attributes.getValue("size")?.let {
                                stack.setSize(it)
                            }
                            currentGraph?.addStack(stack)
                        }
                    }
                    if ("format" == qName) {
                        currentPlot?.setBase(attributes.getValue("base"))
                        currentPlot?.setFactor(attributes.getValue("factor"))
                    }
                    if ("format" == qName) {
                        currentStack?.setBase(attributes.getValue("base"))
                        currentStack?.setFactor(attributes.getValue("factor"))
                    }
                }
            }
        }
    }

    @Throws(SAXException::class)
    override fun endElement(uri: String, localName: String, qName: String) {
        // clean up tempval;
        tempval = tempval.trim { it <= ' ' }
        if ("ConfiG" == qName) {
            beenparse = true
        }
        if ("colors" == qName) {
            in_colors = false
        }
        if ("OSType" == qName) {
            currentOS = null
        }
        if ("Stat" == qName) {
            currentStat = null
        }
        if ("Graph" == qName) {
            currentGraph = null
        }
        if ("Cnx" == qName) {
            currentCnx = null
        }
        if ("Plot" == qName) {
            currentPlot = null
        }
        if ("Stack" == qName) {
            currentStack = null
        }
        if ("HostInfo" == qName) {
            in_hostinfo = false
        }
        if ("headerstr" == qName) {
            currentStat?.headerStr = tempval
        }
        if ("graphname" == qName) {
            currentStat?.graphName = tempval
        }
        if ("duplicate" == qName) {
            currentStat?.setDuplicateTime(tempval)
        }
        if ("cols" == qName) {
            currentPlot?.setHeaderStr(tempval)
            currentStack?.setHeaderStr(tempval)
        }
        if ("range" == qName) {
            currentPlot?.setRange(tempval)
            currentStack?.setRange(tempval)
        }
        if ("itemcolor" == qName) {
            if (currentColor!!.isValid()) {
                colorlist[currentColor!!.data_title] = currentColor!!
            } else {
                // log.error("Err: {}", currentColor.getError_message());
                currentColor = null
            }
            in_color = false
        }
        if (in_color) {
            if ("color" == qName) {
                currentColor?.setDataColor(tempval)
            }
        }
        if (in_cnx) {
            if ("command" == qName) {
                currentCnx?.addCommand(tempval)
            }
        }
        if ("cnx" == qName) {
            if (currentCnx!!.isValid) {
                historyList[currentCnx!!.link] = currentCnx!!
            } else {
                log.error("Err cnx is not valid")
                currentCnx = null
            }
        }
        if (in_hostinfo) {
            if ("alias" == qName) {
                currentHost!!.alias = tempval
            }
            if ("description" == qName) {
                currentHost!!.description = tempval
            }
            if ("memblocksize" == qName) {
                currentHost!!.setMemBlockSize(tempval)
            }
        }
        if ("host" == qName) {
            hostInfoList[currentHost!!.hostname] = currentHost!!
            currentHost = null
        }
    }
}
