/*
 * Copyright 2008 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar

import com.jcraft.jsch.*
import net.atomique.ksar.GlobalOptions.addHistory
import net.atomique.ksar.GlobalOptions.getHistory
import net.atomique.ksar.GlobalOptions.hasUI
import net.atomique.ksar.GlobalOptions.historyList
import net.atomique.ksar.GlobalOptions.uI
import net.atomique.ksar.xml.CnxHistory
import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.event.ActionEvent
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import javax.swing.*

class SSHCommand : Thread {
    private var Cancelbutton: JButton? = null
    private var ComboBoxLabel: JLabel? = null
    private var ConnectionButton: JButton? = null
    private var HostComboBox: JComboBox<String>? = null
    private var HostnameLabel: JLabel? = null
    private var PasswordField: JPasswordField? = null
    private var PasswordLabel: JLabel? = null
    private var buttonpanel: JPanel? = null
    private var commandComboBox: JComboBox<String>? = null
    private var commandPanel: JPanel? = null
    private var headerpanel: JPanel? = null
    private var hostnamePanel: JPanel? = null
    private var infopanel: JPanel? = null
    private val passwordPanel = JPanel()
    private var titleLabel: JLabel? = null

    private val userhostModel = DefaultComboBoxModel<String>()
    private val commandModel = DefaultComboBoxModel<String>()
    private var command: String? = null
    private var mysar: kSar
    private val dialog = JDialog()
    private var jsch: JSch? = null
    private var session: Session? = null
    private var channel: Channel? = null
    private var num_try = 0
    private var `in`: InputStream? = null
    private var err: InputStream? = null
    var password: String? = null

    companion object {
        private val log = LoggerFactory.getLogger(SSHCommand::class.java)
    }

    constructor(hissar: kSar, cmd: String?) {
        mysar = hissar
        command = cmd
    }

    constructor(hissar: kSar) {
        mysar = hissar
        showDialog()
    }

    fun get_action(): String? {
        return if (command != null) {
            "ssh://$command"
        } else {
            null
        }
    }

    private fun showDialog() {
        val ite: Iterator<String> = historyList.keys.iterator()
        while (ite.hasNext()) {
            val tmp = getHistory(ite.next())
            userhostModel.addElement(tmp!!.link)
        }
        initComponents(dialog)
        if (HostComboBox!!.itemCount > 0) {
            HostComboBox!!.selectedIndex = 0
        }
        dialog.setLocationRelativeTo(uI)
        dialog.isVisible = true
    }

    private fun initComponents(dialog: JDialog) {
        headerpanel = JPanel()
        titleLabel = JLabel()
        infopanel = JPanel()
        hostnamePanel = JPanel()
        HostnameLabel = JLabel()
        HostComboBox = JComboBox()
        PasswordLabel = JLabel()
        PasswordField = JPasswordField()
        commandPanel = JPanel()
        ComboBoxLabel = JLabel()
        commandComboBox = JComboBox()
        buttonpanel = JPanel()
        Cancelbutton = JButton()
        ConnectionButton = JButton()
        dialog.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        dialog.modalityType = Dialog.ModalityType.DOCUMENT_MODAL
        titleLabel!!.font = Font("Tahoma", Font.BOLD, 14)
        titleLabel!!.text = "SSH Connection"
        headerpanel!!.add(titleLabel)
        dialog.contentPane.add(headerpanel, BorderLayout.PAGE_START)
        infopanel!!.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        infopanel!!.layout = BoxLayout(infopanel, BoxLayout.PAGE_AXIS)
        hostnamePanel!!.layout = FlowLayout(FlowLayout.LEFT)
        HostnameLabel!!.labelFor = HostComboBox
        HostnameLabel!!.text = "User@Host"
        hostnamePanel!!.add(HostnameLabel)
        HostComboBox!!.isEditable = true
        HostComboBox!!.model = userhostModel
        HostComboBox!!.minimumSize = Dimension(159, 27)
        HostComboBox!!.preferredSize = Dimension(159, 27)
        HostComboBox!!.addActionListener { evt -> HostComboBoxActionPerformed(evt) }
        hostnamePanel!!.add(HostComboBox)
        infopanel!!.add(hostnamePanel)
        passwordPanel.layout = FlowLayout(FlowLayout.LEFT)
        PasswordLabel!!.labelFor = PasswordField
        PasswordLabel!!.text = "Password"
        PasswordLabel!!.preferredSize = Dimension(69, 16)
        passwordPanel.add(PasswordLabel)
        PasswordField!!.minimumSize = Dimension(100, 20)
        PasswordField!!.preferredSize = Dimension(120, 20)
        passwordPanel.add(PasswordField)
        infopanel!!.add(passwordPanel)
        commandPanel!!.layout = FlowLayout(FlowLayout.LEFT)
        ComboBoxLabel!!.labelFor = commandComboBox
        ComboBoxLabel!!.text = "Command"
        ComboBoxLabel!!.preferredSize = Dimension(69, 16)
        commandPanel!!.add(ComboBoxLabel)
        commandComboBox!!.isEditable = true
        commandComboBox!!.model = commandModel
        commandComboBox!!.preferredSize = Dimension(150, 20)
        commandPanel!!.add(commandComboBox)
        infopanel!!.add(commandPanel)
        dialog.contentPane.add(infopanel, BorderLayout.CENTER)
        Cancelbutton!!.text = "Cancel"
        Cancelbutton!!.addActionListener { evt -> CancelbuttonActionPerformed(evt) }
        buttonpanel!!.add(Cancelbutton)
        ConnectionButton!!.text = "Connection"
        ConnectionButton!!.addActionListener { evt -> ConnectionButtonActionPerformed(evt) }
        buttonpanel!!.add(ConnectionButton)
        dialog.contentPane.add(buttonpanel, BorderLayout.SOUTH)
        dialog.pack()
    }

    private fun CancelbuttonActionPerformed(evt: ActionEvent) {
        dialog.dispose()
    }

    private fun ConnectionButtonActionPerformed(evt: ActionEvent) {
        dialog.dispose()
        connect()
    }

    private fun HostComboBoxActionPerformed(evt: ActionEvent) {
        val cb = evt.source as JComboBox<*>
        val link = cb.selectedItem as String
        val tmp = getHistory(link)
        commandModel.removeAllElements()
        if (tmp != null) {
            val ite: Iterator<String> = tmp.commandList.iterator()
            while (ite.hasNext()) {
                commandModel.addElement(ite.next())
            }
        } else {
            commandModel.addElement("sar -A")
        }
    }

    private fun connect() {
        val tmp = CnxHistory(HostComboBox!!.selectedItem as String)
        tmp.addCommand(commandComboBox!!.selectedItem as String)
        jsch = JSch()
        try {
            session = jsch!!.getSession(tmp.username, tmp.hostname, tmp.portInt)
        } catch (ex: JSchException) {
            log.error("JSchException ", ex)
        }
        val config = Properties()
        config["StrictHostKeyChecking"] = "no"
        session!!.setConfig(config)
        val response = CharArray(PasswordField!!.password.size)
        val t = StringBuilder()
        for (i in PasswordField!!.password.indices) {
            response[i] = PasswordField!!.password[i]
            t.append(response[i])
        }
        password = t.toString()
        session!!.setPassword(t.toString())
        val ui: UserInfo = MyUserInfo()
        session!!.userInfo = ui
        // session.setPassword(t.toString());
        try {
            session!!.connect()
        } catch (ex: JSchException) {
            if (hasUI()) {
                JOptionPane.showMessageDialog(
                    uI, "Unable to connect", "SSH error",
                    JOptionPane.ERROR_MESSAGE
                )
                mysar.cleared()
            } else {
                log.error("Err: unable to connect")
            }
            return
        }
        val channel = try {
            session!!.openChannel("exec")
        } catch (ex: JSchException) {
            if (hasUI()) {
                JOptionPane.showMessageDialog(
                    uI, "Unable to open Channel", "SSH error",
                    JOptionPane.ERROR_MESSAGE
                )
                mysar.cleared()
            } else {
                log.error("Err: unable to open Channel")
            }
            return
        }
        this.channel = channel
        (channel as ChannelExec?)!!.setCommand(
            """
    LC_ALL=C ${commandComboBox!!.selectedItem}

            """.trimIndent()
        )
        channel.setInputStream(null)
        channel.setXForwarding(false)
        // ((ChannelExec) channel).setErrStream(err);
        try {
            `in` = channel.getInputStream()
            err = channel.errStream
        } catch (ex: IOException) {
            if (hasUI()) {
                JOptionPane.showMessageDialog(
                    uI, "Unable to open pipe", "SSH error",
                    JOptionPane.ERROR_MESSAGE
                )
                mysar.cleared()
            } else {
                log.error("Err: unable to open pipe")
            }
            return
        }
        try {
            channel.connect()
        } catch (ex: JSchException) {
            if (hasUI()) {
                JOptionPane.showMessageDialog(
                    uI, "Unable to connect Channel",
                    "SSH error", JOptionPane.ERROR_MESSAGE
                )
                mysar.cleared()
            } else {
                log.error("Err: unable to connect Channel")
            }
            return
        }
        if (channel.isClosed()) {
            log.info("exit {}", channel.getExitStatus())
            if (channel.getExitStatus() != 0) {
                if (hasUI()) {
                    JOptionPane.showMessageDialog(
                        uI,
                        "There was a problem while retrieving stat", "SSH error", JOptionPane.ERROR_MESSAGE
                    )
                    mysar.cleared()
                } else {
                    log.error("Err: Problem during ssh connection")
                }
                return
            }
        }
        command = tmp.username + "@" + tmp.hostname + "=" + commandComboBox!!.selectedItem
        addHistory(tmp)
    }

    override fun run() {
        val tmpmessage = StringBuilder()
        var max_waitdata = 10
        try {
            if (`in` == null) {
                return
            }
            // old fashion lead to error;
            //  wait for channel ready
            val tmpin = InputStreamReader(`in`)
            val tmperr = InputStreamReader(err)
            while (max_waitdata > 0 && !tmpin.ready()) {
                // no data and not in timeout
                try {
                    sleep(100)
                } catch (ignored: Exception) {
                }
                max_waitdata--
            }
            val myfile = BufferedReader(tmpin)
            val myerror = BufferedReader(tmperr)
            mysar.parse(myfile)
            var current_line: String?
            while (myerror.readLine().also { current_line = it } != null) {
                tmpmessage.append(current_line)
                tmpmessage.append("\n")
            }
            if (tmpmessage.isNotEmpty()) {
                if (hasUI()) {
                    JOptionPane.showMessageDialog(
                        uI, tmpmessage.toString(), "SSH error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
            myfile.close()
            myerror.close()
            tmpin.close()
            tmperr.close()
            `in`!!.close()
            err!!.close()
            channel!!.disconnect()
            session!!.disconnect()
            channel = null
            session = null
        } catch (ex: Exception) {
            ex.printStackTrace()
            log.error("Exception", ex)
        }
    }

    inner class MyUserInfo : UserInfo, UIKeyboardInteractive {
        override fun promptYesNo(str: String): Boolean {
            val options = arrayOf("yes", "no")
            return if (hasUI()) {
                val foo = JOptionPane.showOptionDialog(
                    uI, str, "Warning",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]
                )
                foo == 0
            } else {
                true
            }
        }

        var passwd: String? = null
        var passwordField: JTextField = JPasswordField(20)
        private var passPhrase: String? = null
        var passphraseField: JTextField = JPasswordField(20)

        override fun getPassphrase() = passPhrase

        override fun getPassword() = passwd

        override fun promptPassphrase(message: String): Boolean {
            return if (num_try > 0) {
                val ob = arrayOf<Any>(passphraseField)
                val result = JOptionPane.showConfirmDialog(
                    uI, ob, message,
                    JOptionPane.OK_CANCEL_OPTION
                )
                if (result == JOptionPane.OK_OPTION) {
                    passPhrase = passphraseField.text
                    true
                } else {
                    false
                }
            } else {
                num_try++
                true
            }
        }

        override fun promptPassword(message: String): Boolean {
            return if (num_try > 0 || password != null) {
                val ob = arrayOf<Any>(passwordField)
                if (hasUI()) {
                    val result = JOptionPane.showConfirmDialog(
                        uI, ob, message,
                        JOptionPane.OK_CANCEL_OPTION
                    )
                    if (result == JOptionPane.OK_OPTION) {
                        passwd = passwordField.text
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            } else {
                num_try++
                true
            }
        }

        val gbc = GridBagConstraints(
            0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
            GridBagConstraints.NONE, Insets(0, 0, 0, 0), 0, 0
        )

        override fun promptKeyboardInteractive(
            destination: String,
            name: String,
            instruction: String,
            prompt: Array<String>,
            echo: BooleanArray
        ): Array<String>? {
            passwd?.let {
                num_try++
                return it.split(".").toTypedArray()
            }
            val panel = JPanel()
            panel.layout = GridBagLayout()
            gbc.weightx = 1.0
            gbc.gridwidth = GridBagConstraints.REMAINDER
            gbc.gridx = 0
            panel.add(JLabel(instruction), gbc)
            gbc.gridy++
            gbc.gridwidth = GridBagConstraints.RELATIVE
            val texts = mutableListOf<JTextField>()
            for (i in prompt.indices) {
                gbc.fill = GridBagConstraints.NONE
                gbc.gridx = 0
                gbc.weightx = 1.0
                panel.add(JLabel(prompt[i]), gbc)
                gbc.gridx = 1
                gbc.fill = GridBagConstraints.HORIZONTAL
                gbc.weighty = 1.0
                texts.add(
                    if (echo[i]) {
                        JTextField(20)
                    } else {
                        JPasswordField(20)
                    }
                )
                panel.add(texts[i], gbc)
                gbc.gridy++
            }
            return if (JOptionPane.showConfirmDialog(
                    uI, panel, "$destination : $name",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE
                ) == JOptionPane.OK_OPTION
            ) {
                texts.map { it.text }.toTypedArray()
            } else {
                null // cancel
            }
        }

        override fun showMessage(message: String) {
            if (hasUI()) {
                JOptionPane.showMessageDialog(uI, message)
            }
        }
    }
}
