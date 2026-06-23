/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.JFileChooser;

public class FileRead extends Thread {

  private static final org.slf4j.Logger log = LoggerFactory.getLogger(FileRead.class);

  public FileRead(kSar hissar) {
    mysar = hissar;
    JFileChooser fc = new JFileChooser();
    if (Config.getLastReadDirectory() != null) {
      fc.setCurrentDirectory(Config.getLastReadDirectory());
    }
    int returnVal = fc.showDialog(GlobalOptions.getUI(), "Open");
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      sarfilename = fc.getSelectedFile().getAbsolutePath();
      if (fc.getSelectedFile().isDirectory()) {
        Config.setLastReadDirectory(fc.getSelectedFile());
      } else {
        Config.setLastReadDirectory(fc.getSelectedFile().getParentFile());
      }
      Config.save();
    }
  }

  public FileRead(kSar hissar, String filename) {
    mysar = hissar;
    sarfilename = filename;
  }

  public String get_action() {
    if (sarfilename != null) {
      return "file://" + sarfilename;
    } else {
      return null;
    }
  }

  private void close() {
    try {
      if (myfilereader != null) {
        myfilereader.close();
      }
      if (tmpfile != null) {
        tmpfile.close();
      }
    } catch (IOException ex) {
      log.error("IO Exception", ex);
    }
  }

  public void run() {
    if (sarfilename == null) {
      return;
    }

    try {
      tmpfile = new FileReader(sarfilename);

      log.debug("FileEncoding : {}", tmpfile.getEncoding());

    } catch (FileNotFoundException ex) {
      log.error("IO Exception", ex);
    }

    myfilereader = new BufferedReader(tmpfile);

    mysar.parse(myfilereader);

    close();
  }

  private kSar mysar = null;
  private String sarfilename = null;
  private FileReader tmpfile = null;
  private BufferedReader myfilereader = null;
}
