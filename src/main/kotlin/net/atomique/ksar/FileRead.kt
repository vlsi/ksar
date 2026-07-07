/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar

import net.atomique.ksar.Config.lastReadDirectory
import net.atomique.ksar.Config.setLastReadDirectory
import net.atomique.ksar.Config.save
import javax.swing.JFileChooser
import org.slf4j.LoggerFactory
import java.io.*

class FileRead(private val mysar: kSar) : Thread() {
    private var sarfilename: String? = null

    companion object {
        private val log = LoggerFactory.getLogger(FileRead::class.java)
    }

    init {
        val fc = JFileChooser()
        if (lastReadDirectory != null) {
            fc.currentDirectory = lastReadDirectory
        }
        val returnVal = fc.showDialog(GlobalOptions.uI, "Open")
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            sarfilename = fc.selectedFile.absolutePath
            if (fc.selectedFile.isDirectory) {
                setLastReadDirectory(fc.selectedFile)
            } else {
                setLastReadDirectory(fc.selectedFile.parentFile)
            }
            save()
        }
    }

    constructor(hissar: kSar, filename: String) : this(hissar) {
        sarfilename = filename
    }

    fun get_action(): String? {
        return if (sarfilename != null) {
            "file://$sarfilename"
        } else {
            null
        }
    }

    override fun run() {
        if (sarfilename == null) {
            return
        }
        try {
            File(sarfilename).bufferedReader().use {
                mysar.parse(it)
            }
        } catch (ex: FileNotFoundException) {
            log.error("IO Exception", ex)
        }
    }
}
