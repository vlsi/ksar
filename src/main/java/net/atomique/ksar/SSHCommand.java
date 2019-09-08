/*
 * Copyright 2008 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;
import net.atomique.ksar.xml.CnxHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class SSHCommand extends Thread {

  private static final Logger log = LoggerFactory.getLogger(SSHCommand.class);

  public SSHCommand(kSar hissar, String cmd) {
    mysar = hissar;
    command = cmd;
  }

  public SSHCommand(kSar hissar) {
    mysar = hissar;
    showDialog();
  }

  public String get_action() {
    if (command != null) {
      return "ssh://" + command;
    } else {
      return null;
    }
  }

  private void showDialog() {

    Iterator<String> ite = GlobalOptions.getHistoryList().keySet().iterator();
    while (ite.hasNext()) {
      CnxHistory tmp = GlobalOptions.getHistory(ite.next());
      userhostModel.addElement(tmp.getLink());
    }
    initComponents(dialog);
    if (HostComboBox.getItemCount() > 0) {
      HostComboBox.setSelectedIndex(0);
    }
    dialog.setLocationRelativeTo(GlobalOptions.getUI());
    dialog.setVisible(true);
  }

  private void initComponents(JDialog dialog) {

    headerpanel = new javax.swing.JPanel();
    titleLabel = new javax.swing.JLabel();
    infopanel = new javax.swing.JPanel();
    hostnamePanel = new javax.swing.JPanel();
    HostnameLabel = new javax.swing.JLabel();
    HostComboBox = new javax.swing.JComboBox();
    passwordPanel = new javax.swing.JPanel();
    PasswordLabel = new javax.swing.JLabel();
    PasswordField = new javax.swing.JPasswordField();
    commandPanel = new javax.swing.JPanel();
    ComboBoxLabel = new javax.swing.JLabel();
    commandComboBox = new javax.swing.JComboBox();
    buttonpanel = new javax.swing.JPanel();
    Cancelbutton = new javax.swing.JButton();
    ConnectionButton = new javax.swing.JButton();

    dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.setModalityType(java.awt.Dialog.ModalityType.DOCUMENT_MODAL);

    titleLabel.setFont(new java.awt.Font("Tahoma", Font.BOLD, 14));
    titleLabel.setText("SSH Connection");
    headerpanel.add(titleLabel);

    dialog.getContentPane().add(headerpanel, java.awt.BorderLayout.PAGE_START);

    infopanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
    infopanel.setLayout(new javax.swing.BoxLayout(infopanel, javax.swing.BoxLayout.PAGE_AXIS));

    hostnamePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

    HostnameLabel.setLabelFor(HostComboBox);
    HostnameLabel.setText("User@Host");
    hostnamePanel.add(HostnameLabel);

    HostComboBox.setEditable(true);
    HostComboBox.setModel(userhostModel);
    HostComboBox.setMinimumSize(new java.awt.Dimension(159, 27));
    HostComboBox.setPreferredSize(new java.awt.Dimension(159, 27));
    HostComboBox.addActionListener(new java.awt.event.ActionListener() {

      public void actionPerformed(java.awt.event.ActionEvent evt) {
        HostComboBoxActionPerformed(evt);
      }
    });
    hostnamePanel.add(HostComboBox);

    infopanel.add(hostnamePanel);

    passwordPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

    PasswordLabel.setLabelFor(PasswordField);
    PasswordLabel.setText("Password");
    PasswordLabel.setPreferredSize(new java.awt.Dimension(69, 16));
    passwordPanel.add(PasswordLabel);

    PasswordField.setMinimumSize(new java.awt.Dimension(100, 20));
    PasswordField.setPreferredSize(new java.awt.Dimension(120, 20));
    passwordPanel.add(PasswordField);

    infopanel.add(passwordPanel);

    commandPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

    ComboBoxLabel.setLabelFor(commandComboBox);
    ComboBoxLabel.setText("Command");
    ComboBoxLabel.setPreferredSize(new java.awt.Dimension(69, 16));
    commandPanel.add(ComboBoxLabel);

    commandComboBox.setEditable(true);
    commandComboBox.setModel(commandModel);
    commandComboBox.setPreferredSize(new java.awt.Dimension(150, 20));
    commandPanel.add(commandComboBox);

    infopanel.add(commandPanel);

    dialog.getContentPane().add(infopanel, java.awt.BorderLayout.CENTER);

    Cancelbutton.setText("Cancel");
    Cancelbutton.addActionListener(new java.awt.event.ActionListener() {

      public void actionPerformed(java.awt.event.ActionEvent evt) {
        CancelbuttonActionPerformed(evt);
      }
    });
    buttonpanel.add(Cancelbutton);

    ConnectionButton.setText("Connection");
    ConnectionButton.addActionListener(new java.awt.event.ActionListener() {

      public void actionPerformed(java.awt.event.ActionEvent evt) {
        ConnectionButtonActionPerformed(evt);
      }
    });
    buttonpanel.add(ConnectionButton);

    dialog.getContentPane().add(buttonpanel, java.awt.BorderLayout.SOUTH);

    dialog.pack();
  }

  private void CancelbuttonActionPerformed(java.awt.event.ActionEvent evt) {
    dialog.dispose();
  }

  private void ConnectionButtonActionPerformed(java.awt.event.ActionEvent evt) {
    dialog.dispose();
    connect();
  }

  private void HostComboBoxActionPerformed(java.awt.event.ActionEvent evt) {
    JComboBox cb = (JComboBox) evt.getSource();
    String link = (String) cb.getSelectedItem();
    CnxHistory tmp = GlobalOptions.getHistory(link);
    commandModel.removeAllElements();
    if (tmp != null) {
      Iterator<String> ite = tmp.getCommandList().iterator();
      while (ite.hasNext()) {
        commandModel.addElement(ite.next());
      }
    } else {
      commandModel.addElement("sar -A");
    }
  }

  private void connect() {
    CnxHistory tmp = new CnxHistory((String) HostComboBox.getSelectedItem());
    tmp.addCommand((String) commandComboBox.getSelectedItem());

    jsch = new JSch();
    try {
      session = jsch.getSession(tmp.getUsername(), tmp.getHostname(), tmp.getPortInt());
    } catch (JSchException ex) {
      log.error("JSchException ", ex);
    }

    java.util.Properties config = new java.util.Properties();
    config.put("StrictHostKeyChecking", "no");
    session.setConfig(config);
    char[] response = new char[PasswordField.getPassword().length];
    StringBuilder t = new StringBuilder();

    for (int i = 0; i < PasswordField.getPassword().length; i++) {
      response[i] = PasswordField.getPassword()[i];
      t.append(response[i]);
    }
    password = t.toString();
    session.setPassword(t.toString());
    UserInfo ui = new MyUserInfo();
    session.setUserInfo(ui);
    //session.setPassword(t.toString());
    try {
      session.connect();
    } catch (JSchException ex) {
      if (GlobalOptions.hasUI()) {
        JOptionPane.showMessageDialog(GlobalOptions.getUI(), "Unable to connect", "SSH error",
            JOptionPane.ERROR_MESSAGE);
        mysar.cleared();
      } else {
        log.error("Err: unable to connect");
      }
      return;
    }
    try {
      channel = session.openChannel("exec");
    } catch (JSchException ex) {
      if (GlobalOptions.hasUI()) {
        JOptionPane.showMessageDialog(GlobalOptions.getUI(), "Unable to open Channel", "SSH error",
            JOptionPane.ERROR_MESSAGE);
        mysar.cleared();
      } else {
        log.error("Err: unable to open Channel");
      }
      return;
    }
    ((ChannelExec) channel).setCommand("LC_ALL=C " + commandComboBox.getSelectedItem() + "\n");
    channel.setInputStream(null);
    channel.setXForwarding(false);
    //((ChannelExec) channel).setErrStream(err);

    try {
      in = channel.getInputStream();
      err = ((ChannelExec) channel).getErrStream();
    } catch (IOException ex) {
      if (GlobalOptions.hasUI()) {
        JOptionPane.showMessageDialog(GlobalOptions.getUI(), "Unable to open pipe", "SSH error",
            JOptionPane.ERROR_MESSAGE);
        mysar.cleared();
      } else {
        log.error("Err: unable to open pipe");
      }
      return;
    }
    try {
      channel.connect();
    } catch (JSchException ex) {
      if (GlobalOptions.hasUI()) {
        JOptionPane.showMessageDialog(GlobalOptions.getUI(), "Unable to connect Channel",
            "SSH error", JOptionPane.ERROR_MESSAGE);
        mysar.cleared();
      } else {
        log.error("Err: unable to connect Channel");
      }
      return;
    }
    if (channel.isClosed()) {
      log.info("exit {}", channel.getExitStatus());
      if (channel.getExitStatus() != 0) {
        if (GlobalOptions.hasUI()) {
          JOptionPane.showMessageDialog(GlobalOptions.getUI(),
              "There was a problem while retrieving stat", "SSH error", JOptionPane.ERROR_MESSAGE);
          mysar.cleared();
        } else {
          log.error("Err: Problem during ssh connection");
        }
        return;
      }
    }
    command = tmp.getUsername() + "@" + tmp.getHostname() + "=" + commandComboBox.getSelectedItem();
    GlobalOptions.addHistory(tmp);

  }

  public void run() {
    StringBuilder tmpmessage = new StringBuilder();
    int max_waitdata = 10;

    try {
      if (in == null) {
        return;
      }
      // old fashion lead to error;
      //  wait for channel ready
      InputStreamReader tmpin = new InputStreamReader(in);
      InputStreamReader tmperr = new InputStreamReader(err);

      while (max_waitdata > 0 && !tmpin.ready()) {
        // no data and not in timeout
        try {
          Thread.sleep(100);
        } catch (Exception ignored) {
        }
        max_waitdata--;
      }

      BufferedReader myfile = new BufferedReader(tmpin);
      BufferedReader myerror = new BufferedReader(tmperr);

      mysar.parse(myfile);
      String current_line;

      while ((current_line = myerror.readLine()) != null) {
        tmpmessage.append(current_line);
        tmpmessage.append("\n");
      }
      if (tmpmessage.length() > 0) {
        if (GlobalOptions.hasUI()) {
          JOptionPane.showMessageDialog(GlobalOptions.getUI(), tmpmessage.toString(), "SSH error",
              JOptionPane.ERROR_MESSAGE);
        }
      }

      myfile.close();
      myerror.close();
      tmpin.close();
      tmperr.close();
      in.close();
      err.close();

      channel.disconnect();
      session.disconnect();
      channel = null;
      session = null;

    } catch (Exception ex) {
      ex.printStackTrace();
      log.error("Exception", ex);
    }
  }

  public class MyUserInfo implements UserInfo, UIKeyboardInteractive {

    public boolean promptYesNo(String str) {

      String[] options = {"yes", "no"};
      if (GlobalOptions.hasUI()) {
        int foo = JOptionPane.showOptionDialog(GlobalOptions.getUI(), str, "Warning",
            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        return foo == 0;
      } else {
        return true;
      }
    }

    String passwd;
    JTextField passwordField = (JTextField) new JPasswordField(20);
    String passphrase;
    JTextField passphraseField = (JTextField) new JPasswordField(20);

    public String getPassphrase() {
      return passphrase;
    }

    public boolean promptPassphrase(String message) {
      if (num_try > 0) {
        Object[] ob = {passphraseField};
        int result = JOptionPane.showConfirmDialog(GlobalOptions.getUI(), ob, message,
            JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
          passphrase = passphraseField.getText();
          return true;
        } else {
          return false;
        }
      } else {
        num_try++;
        return true;
      }
    }

    public String getPassword() {
      if (password != null) {
        return password;
      }
      return passwd;
    }

    public boolean promptPassword(String message) {
      if (num_try > 0 || password != null) {
        Object[] ob = {passwordField};
        if (GlobalOptions.hasUI()) {
          int result = JOptionPane.showConfirmDialog(GlobalOptions.getUI(), ob, message,
              JOptionPane.OK_CANCEL_OPTION);
          if (result == JOptionPane.OK_OPTION) {
            passwd = passwordField.getText();
            return true;
          } else {
            return false;
          }
        } else {
          return false;
        }
      } else {
        num_try++;
        return true;
      }
    }

    final GridBagConstraints gbc =
        new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST,
            GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    private Container panel;

    public String[] promptKeyboardInteractive(String destination, String name, String instruction,
        String[] prompt, boolean[] echo) {

      if (password != null) {
        num_try++;
        return password.split("[.]");
      }
      panel = new JPanel();
      panel.setLayout(new GridBagLayout());

      gbc.weightx = 1.0;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.gridx = 0;
      panel.add(new JLabel(instruction), gbc);
      gbc.gridy++;

      gbc.gridwidth = GridBagConstraints.RELATIVE;

      JTextField[] texts = new JTextField[prompt.length];
      for (int i = 0; i < prompt.length; i++) {
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.weightx = 1;
        panel.add(new JLabel(prompt[i]), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1;
        if (echo[i]) {
          texts[i] = new JTextField(20);
        } else {
          texts[i] = new JPasswordField(20);
        }
        panel.add(texts[i], gbc);
        gbc.gridy++;
      }

      if (JOptionPane.showConfirmDialog(GlobalOptions.getUI(), panel, destination + " : " + name,
          JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
        String[] response = new String[prompt.length];

        for (int i = 0; i < prompt.length; i++) {
          response[i] = texts[i].getText();
        }
        return response;
      } else {
        return null;  // cancel
      }
    }

    public void showMessage(String message) {
      if (GlobalOptions.hasUI()) {
        JOptionPane.showMessageDialog(GlobalOptions.getUI(), message);
      }
    }
  }

  // Variables declaration - do not modify
  private javax.swing.JButton Cancelbutton;
  private javax.swing.JLabel ComboBoxLabel;
  private javax.swing.JButton ConnectionButton;
  private javax.swing.JComboBox HostComboBox;
  private javax.swing.JLabel HostnameLabel;
  private javax.swing.JPasswordField PasswordField;
  private javax.swing.JLabel PasswordLabel;
  private javax.swing.JPanel buttonpanel;
  private javax.swing.JComboBox commandComboBox;
  private javax.swing.JPanel commandPanel;
  private javax.swing.JPanel headerpanel;
  private javax.swing.JPanel hostnamePanel;
  private javax.swing.JPanel infopanel;
  private javax.swing.JPanel passwordPanel;
  private javax.swing.JLabel titleLabel;
  // End of variables declaration
  private DefaultComboBoxModel userhostModel = new DefaultComboBoxModel();
  private DefaultComboBoxModel commandModel = new DefaultComboBoxModel();
  private String command;
  private kSar mysar;
  private JDialog dialog = new JDialog();
  private JSch jsch = null;
  private Session session = null;
  private Channel channel = null;
  private int num_try = 0;
  private InputStream in = null;
  private InputStream err = null;
  String password = null;
}
