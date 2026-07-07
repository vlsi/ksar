/*
 * Copyright 2008 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar

import net.atomique.ksar.GlobalOptions.uI
import net.atomique.ksar.GlobalOptions.hasUI
import javax.swing.JOptionPane
import java.lang.ProcessBuilder
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.Process

class LocalCommand internal constructor(private val mysar: kSar) : Thread() {
    private var `in`: InputStream? = null
    private var command: String? = null
    private var p: Process? = null

    companion object {
        private val log = LoggerFactory.getLogger(LocalCommand::class.java)
    }

    init {
        try {
            command = JOptionPane.showInputDialog(uI, "Enter local command ", "sar -A")
            val cmdList = command!!.split(Regex(" +"))
            val pb = ProcessBuilder(cmdList)
            pb.environment()["LC_ALL"] = "C"
            p = pb.start().also {
                `in` = it.inputStream
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                uI, "There was a problem while running the command ",
                "Local error", JOptionPane.ERROR_MESSAGE
            )
            `in` = null
        }
    }

    internal constructor(mysar: kSar, hiscommand: String?) : this(mysar) {
        command = hiscommand
        try {
            val envvar: Array<String?>
            envvar = arrayOfNulls(1)
            envvar[0] = "LC_ALL=C"
            p = Runtime.getRuntime().exec(command, envvar).also {
                `in` = it.inputStream
            }
        } catch (e: Exception) {
            if (hasUI()) {
                JOptionPane.showMessageDialog(
                    uI,
                    "There was a problem while running the command $command", "Local error",
                    JOptionPane.ERROR_MESSAGE
                )
            } else {
                log.error("There was a problem while running the command {}", command)
            }
            `in` = null
        }
    }

    private fun close() {
        p?.destroy()
    }

    override fun run() {
        if (`in` == null) {
            return
        }
        try {
            val myfilereader = BufferedReader(InputStreamReader(`in`))
            mysar.parse(myfilereader)
            myfilereader.close()
        } catch (ex: IOException) {
            log.error("IO Exception", ex)
        }
        close()
    }

    fun get_action(): String? {
        return if (command != null) {
            "cmd://$command"
        } else {
            null
        }
    }
}
