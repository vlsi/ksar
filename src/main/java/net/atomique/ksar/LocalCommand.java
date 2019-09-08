/*
 * Copyright 2008 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

public class LocalCommand extends Thread {

  private static final Logger log = LoggerFactory.getLogger(LocalCommand.class);

  LocalCommand(kSar hissar) {
    mysar = hissar;
    try {
      command = JOptionPane.showInputDialog(GlobalOptions.getUI(), "Enter local command ", "sar -A");
      if (command == null) {
        return;
      }
      String[] cmdArray = command.split(" +");
      List<String> cmdList = new ArrayList<String>();
      cmdList.addAll(Arrays.asList(cmdArray));
      ProcessBuilder pb = new ProcessBuilder(cmdList);
      pb.environment().put("LC_ALL", "C");
      p = pb.start();
      in = p.getInputStream();
    } catch (Exception e) {
      JOptionPane.showMessageDialog(GlobalOptions.getUI(), "There was a problem while running the command ",
          "Local error", JOptionPane.ERROR_MESSAGE);
      in = null;
    }

  }

  LocalCommand(kSar hissar, String hiscommand) {
    mysar = hissar;
    command = hiscommand;
    try {
      String[] envvar;
      envvar = new String[1];
      envvar[0] = "LC_ALL=C";

      p = Runtime.getRuntime().exec(command, envvar);
      in = p.getInputStream();
    } catch (Exception e) {
      if (GlobalOptions.hasUI()) {
        JOptionPane.showMessageDialog(GlobalOptions.getUI(),
            "There was a problem while running the command " + command, "Local error",
            JOptionPane.ERROR_MESSAGE);
      } else {
        log.error("There was a problem while running the command {}", command);
      }
      in = null;
    }

  }

  private void close() {
    if (p != null) {
      p.destroy();
    }

    try {
      if (myfilereader != null) {
        myfilereader.close();
      }
    } catch (IOException ex) {
      log.error("IO Exception", ex);
    }
  }

  public void run() {
    String current_line;

    if (in == null) {
      return;
    }
    myfilereader = new BufferedReader(new InputStreamReader(in));
    if (myfilereader == null) {
      return;
    }

    mysar.parse(myfilereader);

    close();
  }

  String get_action() {
    if (command != null) {
      return "cmd://" + command;
    } else {
      return null;
    }
  }

  private kSar mysar;
  private InputStream in = null;
  private String command = null;
  private BufferedReader myfilereader = null;
  private Process p = null;
}
