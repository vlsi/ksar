/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.atomique.ksar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Max
 */
public class LocalCommand extends Thread {

    public LocalCommand(kSar hissar) {
        mysar = hissar;
        try {
            command = JOptionPane.showInputDialog("Enter local command ", "sar -A");
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
            JOptionPane.showMessageDialog(null, "There was a problem while running the command ", "Local error", JOptionPane.ERROR_MESSAGE);
            in = null;
        }

        return;
    }

    public LocalCommand(kSar hissar, String hiscommand) {
        mysar = hissar;
        command = hiscommand;
        try {
            String[] envvar;
            envvar = new String[1];
            envvar[0] = "LC_ALL=C";

            p = Runtime.getRuntime().exec(command, envvar);
            in = p.getInputStream();
        } catch (Exception e) {
            if (GlobalOptions.hasUI() ) {
                JOptionPane.showMessageDialog(GlobalOptions.getUI(), "There was a problem while running the command " + command, "Local error", JOptionPane.ERROR_MESSAGE);
            } else {
                System.err.println("There was a problem while running the command " + command);
            }
            in = null;
        }

        return;
    }

    private void close() {
        if ( p != null ) {
            p.destroy();
        }

        try {
            if (myfilereader != null) {
                myfilereader.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(LocalCommand.class.getName()).log(Level.SEVERE, null, ex);
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

    public String get_action() {
        if ( command != null ) {
        return "cmd://" + command;
        } else {
            return null;
        }
    }
    private kSar mysar = null;
    private InputStream in = null;
    private String command = null;
    private BufferedReader myfilereader = null;
    private Process p = null;
}
