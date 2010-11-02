/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.atomique.ksar;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;

/**
 *
 * @author Max
 */
public class FileRead extends Thread {

    public FileRead(kSar hissar) {
        mysar = hissar;
        JFileChooser fc = new JFileChooser();
        if (Config.getLastReadDirectory() != null) {
            fc.setCurrentDirectory(Config.getLastReadDirectory());
        }
        int returnVal = fc.showDialog(null, "Open");
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
        if ( sarfilename != null ) {
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
            Logger.getLogger(FileRead.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void run() {
        if (sarfilename == null) {
            return;
        }

        try {
            tmpfile = new FileReader(sarfilename);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileRead.class.getName()).log(Level.SEVERE, null, ex);
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
