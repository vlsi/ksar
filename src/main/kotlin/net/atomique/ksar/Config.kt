/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar

import org.slf4j.LoggerFactory
import java.awt.Font
import java.io.File
import java.io.IOException
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences
import javax.swing.UIManager

object Config {
    private val log = LoggerFactory.getLogger(GlobalOptions::class.java)
    private val myPref: Preferences

    @JvmStatic
    var landf: String? = null

    @JvmStatic
    var lastReadDirectory: File? = null
        private set
    private var lastExportDirectory: File? = null
    var lastCommand: String? = null
    private var number_host_history = 0
    private var local_configfile = 0
    val host_history = arrayListOf<String>()
    val dEFAULT_FONT = Font("SansSerif", Font.BOLD, 18)
    @JvmStatic
    lateinit var linuxDateFormat: String
    @JvmStatic
    var pDFPageFormat: String? = null
    @JvmStatic
    var imageWidth = 0
    @JvmStatic
    var imageHeight = 0

    init {
        log.trace("load Config")
        myPref = Preferences.userNodeForPackage(Config::class.java)
        if (myPref.getInt("local_configfile", -1) == -1) {
            // new
            try {
                myPref.clear()
                myPref.flush()
            } catch (e: BackingStoreException) {
                log.error("BackingStoreException", e)
            }
            local_configfile = store_configdir()
            myPref.putInt("local_configfile", local_configfile)
        }
        load()
    }

    private fun load() {
        /*
* load default value or stored value
*/
        landf = myPref["landf", UIManager.getLookAndFeel().name]
        setLastReadDirectory(myPref["lastReadDirectory", null])
        setLastExportDirectory(myPref["lastExportDirectory", null])
        imageHeight = myPref.getInt("ImageHeight", 600)
        imageWidth = myPref.getInt("ImageWidth", 800)
        pDFPageFormat = myPref["PDFPageFormat", "A4"]
        linuxDateFormat = myPref["LinuxDateFormat", "Always ask"]
        number_host_history = myPref.getInt("HostHistory", 0)
        for (i in 0 until number_host_history) {
            host_history.add(myPref["HostHistory_$i", null])
        }
        local_configfile = myPref.getInt("local_configfile", -1)
    }

    @JvmStatic
    fun save() {
        myPref.put("landf", landf)
        if (lastReadDirectory != null) {
            myPref.put("lastReadDirectory", lastReadDirectory.toString())
        }
        if (lastExportDirectory != null) {
            myPref.put("lastExportDirectory", lastExportDirectory.toString())
        }
        myPref.putInt("ImageHeight", imageHeight)
        myPref.putInt("ImageWidth", imageWidth)
        myPref.put("PDFPageFormat", pDFPageFormat)
        myPref.put("LinuxDateFormat", linuxDateFormat)
        for (i in host_history.indices) {
            myPref.put("HostHistory_$i", host_history[i])
        }
        myPref.putInt("HostHistory", host_history.size)
        myPref.putInt("local_configfile", local_configfile)
    }

    private fun setLastReadDirectory(lastReadDirectory: String?) {
        if (lastReadDirectory != null) {
            this.lastReadDirectory = File(lastReadDirectory)
        }
    }

    @JvmStatic
    fun setLastReadDirectory(lastReadDirectory: File?) {
        this.lastReadDirectory = lastReadDirectory
    }

    @JvmStatic
    fun getLastExportDirectory(): File? {
        return lastReadDirectory
    }

    @JvmStatic
    fun setLastExportDirectory(lastExportDirectory: String?) {
        if (lastExportDirectory != null) {
            this.lastExportDirectory = File(lastExportDirectory)
        }
    }

    @JvmStatic
    fun setLastExportDirectory(lastExportDirectory: File?) {
        this.lastExportDirectory = lastExportDirectory
    }

    fun addHost_history(e: String) {
        host_history.add(e)
    }

    private fun store_configdir(): Int {
        val systemprops = System.getProperties()
        val username = systemprops["user.name"] as String?
        val userhome = username + File.separator
        // mkdir userhome/.ksar
        val buffer = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n\n<ConfiG>\n</ConfiG>\n"
        val home = File("$userhome.ksarcfg").mkdir()
        if (!home) {
            return 0
        }
        return try {
            File("$userhome.ksarcfg${File.separator}Config.xml").writeText(buffer)
            1
        } catch (e: IOException) {
            0
        }
    }
}
