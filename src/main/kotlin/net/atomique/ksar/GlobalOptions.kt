/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar

import net.atomique.ksar.ui.Desktop
import net.atomique.ksar.xml.CnxHistory
import net.atomique.ksar.xml.ColumnConfig
import net.atomique.ksar.xml.HostInfo
import net.atomique.ksar.xml.OSConfig
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*

object GlobalOptions {
    private val log = LoggerFactory.getLogger(GlobalOptions::class.java)

    @JvmStatic
    fun hasUI(): Boolean {
        return uI != null
    }

    fun getColumnConfig(s: String): ColumnConfig? =
        if (colorlist.isEmpty()) {
            null
        } else colorlist[s]

    fun getDataColor(s: String): Color? {
        val tmp = colorlist[s]
        if (tmp != null) {
            return tmp.dataColor
        } else {
            log.warn("color not found for tag {}", s)
        }
        return null
    }

    fun getOSinfo(s: String): OSConfig = oSlist.getValue(s)

    @JvmStatic
    fun getParser(s: String): Class<out OSParser> {
        val tmp = s.replace("-", "")
        return ParserMap.getValue(tmp)
    }

    fun getHostInfo(s: String): HostInfo? {
        return if (hostInfoList.isEmpty()) {
            null
        } else hostInfoList[s]
    }

    @JvmStatic
    fun addHostInfo(s: HostInfo) {
        hostInfoList[s.hostname] = s
        saveHistory()
    }

    @JvmStatic
    fun getHistory(s: String): CnxHistory? {
        return if (historyList.isEmpty()) {
            null
        } else historyList[s]
    }

    @JvmStatic
    fun addHistory(s: CnxHistory) {
        val tmp = historyList[s.link]
        if (tmp != null) {
            val ite: Iterator<String> = s.commandList.iterator()
            while (ite.hasNext()) {
                tmp.addCommand(ite.next())
            }
        } else {
            historyList[s.link] = s
        }
        saveHistory()
    }

    fun saveHistory() {
        val tmpFile: File
        var tmpFileOut: BufferedWriter? = null
        if (historyList.isEmpty() && hostInfoList.isEmpty()) {
            log.info("list is null")
            return
        }
        try {
            tmpFile = File(userhome + ".ksarcfg" + fileseparator + "History.xmltemp")
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            if (tmpFile.createNewFile() && tmpFile.canWrite()) {
                tmpFileOut = BufferedWriter(FileWriter(tmpFile))
            }
            // xml header
            tmpFileOut!!.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<ConfiG>\n")
            tmpFileOut.write("\t<History>\n")
            val ite: Iterator<String> = historyList.keys.iterator()
            while (ite.hasNext()) {
                val tmp = historyList[ite.next()]
                tmpFileOut.write(tmp!!.save())
            }
            // xml footer
            tmpFileOut.write("\t</History>\n")
            tmpFileOut.write("\t<HostInfo>\n")
            val ite2: Iterator<String> = hostInfoList.keys.iterator()
            while (ite2.hasNext()) {
                val tmp = hostInfoList[ite2.next()]
                tmpFileOut.write(tmp!!.save())
            }
            // xml footer
            tmpFileOut.write("\t</HostInfo>\n")
            tmpFileOut.write("</ConfiG>\n")
            tmpFileOut.flush()
            tmpFileOut.close()
            val oldfile = File(userhome + ".ksarcfg" + fileseparator + "History.xml")
            oldfile.delete()
            tmpFile.renameTo(oldfile)
        } catch (ex: IOException) {
            log.error("IO exception", ex)
        }
    }

    @JvmStatic
    var uI: Desktop? = null
    private var systemprops: Properties
    var userhome: String
        private set
    var username: String?
        private set
    var fileseparator: String?
        private set
    @JvmStatic
    var colorlist: HashMap<String, ColumnConfig>
        private set
    @JvmStatic
    var oSlist: HashMap<String, OSConfig>
        private set
    @JvmStatic
    var historyList: HashMap<String, CnxHistory>
        private set
    @JvmStatic
    var hostInfoList: HashMap<String, HostInfo>
        private set
    @JvmStatic
    var cLfilename: String? = null
    private var ParserMap: HashMap<String, Class<out OSParser>>
    private const val firstrun = true

    init {
        log.trace("load GlobalOptions")
        val OSParserNames = arrayOf("AIX", "HPUX", "Linux", "SunOS")
        var filename: String
        val tmp: XMLConfig
        systemprops = System.getProperties()
        username = systemprops["user.name"] as String?
        userhome = systemprops["user.home"] as String? + systemprops["file.separator"]
        fileseparator = systemprops["file.separator"] as String?
        colorlist = HashMap()
        oSlist = HashMap()
        ParserMap = HashMap()
        historyList = HashMap()
        hostInfoList = HashMap()
        tmp = XMLConfig()
        tmp.loadFromResources("/Config.xml")
        for (OSName in OSParserNames) {
            try {
                val tmpclass = Class.forName("net.atomique.ksar.parser.$OSName")
                    .asSubclass(OSParser::class.java)
                ParserMap[OSName] = tmpclass
            } catch (ex: ClassNotFoundException) {
                log.error("Parser class not found", ex)
            }
        }
        for (parsername in ParserMap.keys) {
            tmp.loadFromResources("/$parsername.xml")
        }
        filename = userhome + ".ksarcfg" + fileseparator + "Config.xml"
        var file = File(filename)
        if (file.canRead()) {
            tmp.loadConfig(file)
        }
        filename = userhome + ".ksarcfg" + fileseparator + "History.xml"
        file = File(filename)
        if (file.canRead()) {
            tmp.loadConfig(file)
        }
    }
}
