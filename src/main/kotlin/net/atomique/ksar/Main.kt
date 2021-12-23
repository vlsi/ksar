/*
 * Copyright 2008 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar

import net.atomique.ksar.Config.landf
import net.atomique.ksar.GlobalOptions.cLfilename
import net.atomique.ksar.GlobalOptions.uI
import net.atomique.ksar.ui.Desktop
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.slf4j.LoggerFactory
import java.util.*
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException

object Main {
    private val log = LoggerFactory.getLogger(Main::class.java)
    var config: Config? = null
    var globaloptions: GlobalOptions? = null
    var resource = ResourceBundle.getBundle("net/atomique/ksar/Language/Message")
    fun usage() {
        show_version()
    }

    fun show_version() {
        log.info("ksar Version : {}", VersionNumber.versionString)
    }

    private fun set_lookandfeel() {
        for (laf in UIManager.getInstalledLookAndFeels()) {
            if (landf == laf.name) {
                try {
                    UIManager.setLookAndFeel(laf.className)
                } catch (ex: ClassNotFoundException) {
                    log.error("lookandfeel Exception", ex)
                } catch (ex: InstantiationException) {
                    log.error("lookandfeel Exception", ex)
                } catch (ex: IllegalAccessException) {
                    log.error("lookandfeel Exception", ex)
                } catch (ex: UnsupportedLookAndFeelException) {
                    log.error("lookandfeel Exception", ex)
                }
            }
        }
    }

    fun make_ui() {
        log.trace("MainScreen")
        set_lookandfeel()
        SwingUtilities.invokeLater {
            uI = Desktop()
            SwingUtilities.updateComponentTreeUI(uI)
            uI!!.add_window()
            uI!!.maxall()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        var i = 0
        var arg: String
        log.info("ksar Version : {}", VersionNumber.versionString)
        log.info("Java runtime Version : {}", System.getProperty("java.runtime.version"))
        log.info("Java runtime architecture : {}", System.getProperty("os.arch"))

        // / load default - Mac OS X Application Properties
        val mrjVersion = System.getProperty("mrj.version")
        if (mrjVersion != null) {
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false")
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "kSar")
            System.setProperty("apple.laf.useScreenMenuBar", "true")
        }
        config = Config
        globaloptions = GlobalOptions
        if (args.isNotEmpty()) {
            while (i < args.size && args[i].startsWith("-")) {
                arg = args[i++]
                if ("-version" == arg) {
                    show_version()
                    System.exit(0)
                }
                if ("-help" == arg) {
                    usage()
                    continue
                }
                if ("-test" == arg || "-debug" == arg) {
                    Configurator.setRootLevel(Level.DEBUG)
                    continue
                }
                if ("-trace" == arg) {
                    Configurator.setRootLevel(Level.TRACE)
                    continue
                }
                if ("-input" == arg) {
                    if (i < args.size) {
                        cLfilename = args[i++]
                    } else {
                        exit_error(resource.getString("INPUT_REQUIRE_ARG"))
                    }
                }
            }
        }
        make_ui()
    }

    fun exit_error(message: String?) {
        log.error(message)
        System.exit(1)
    }
}
