/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.atomique.ksar;

import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.UIManager;

/**
 *
 * @author Max
 */
public class Config {

    private static Preferences myPref;
    private static Config instance = new Config();

    public static Config getInstance() {
        return instance;
    }

    Config() {
        myPref = Preferences.userNodeForPackage(Config.class);
        if (myPref.getInt("local_configfile", -1) == -1) {
            // new
            try {
                myPref.clear();
                myPref.flush();
            } catch (BackingStoreException e) {
            }
            local_configfile = store_configdir();
            myPref.putInt("local_configfile", local_configfile);
            
        }
        load();
    }

    private static void load() {
        /*
         * load default value or stored value
         */
        setLandf(myPref.get("landf", UIManager.getLookAndFeel().getName()));
        setLastReadDirectory(myPref.get("lastReadDirectory", null));
        setLastExportDirectory(myPref.get("lastExportDirectory", null));

        setImageHeight(myPref.getInt("ImageHeight", 600));
        setImageWidth(myPref.getInt("ImageWidth", 800));
        setPDFPageFormat(myPref.get("PDFPageFormat","A4"));
        setLinuxDateFormat(myPref.get("LinuxDateFormat","Always ask"));

        setNumber_host_history(myPref.getInt("HostHistory", 0));
        for (int i = 0; i < getNumber_host_history(); i++) {
            host_history.add(myPref.get("HostHistory_" + i, null));
        }
        setLocal_configfile(myPref.getInt("local_configfile", -1));
    }

    public static void save() {
        if (myPref == null) {
            return;
        }
        myPref.put("landf", landf);
        if (lastReadDirectory != null) {
            myPref.put("lastReadDirectory", lastReadDirectory.toString());
        }
        if (lastExportDirectory != null) {
            myPref.put("lastExportDirectory", lastExportDirectory.toString());
        }

        myPref.putInt("ImageHeight", ImageHeight);
        myPref.putInt("ImageWidth", ImageWidth);
        myPref.put("PDFPageFormat", PDFPageFormat);
        myPref.put("LinuxDateFormat", LinuxDateFormat);

        for (int i = 0; i < host_history.size(); i++) {
            myPref.put("HostHistory_" + i, host_history.get(i));
        }
        myPref.putInt("HostHistory", host_history.size());

        myPref.putInt("local_configfile", local_configfile);

    }

    public static String getLandf() {
        return landf;
    }

    public static void setLandf(String landf) {
        Config.landf = landf;
    }

    public static File getLastReadDirectory() {
        return lastReadDirectory;
    }

    public static void setLastReadDirectory(String lastReadDirectory) {
        if (lastReadDirectory != null) {
            Config.lastReadDirectory = new File(lastReadDirectory);
        }
    }

    public static void setLastReadDirectory(File lastReadDirectory) {
        Config.lastReadDirectory = lastReadDirectory;
    }

    public static File getLastExportDirectory() {
        return lastReadDirectory;
    }

    public static void setLastExportDirectory(String lastExportDirectory) {
        if (lastExportDirectory != null) {
            Config.lastExportDirectory = new File(lastExportDirectory);
        }
    }

    public static void setLastExportDirectory(File lastExportDirectory) {
        Config.lastExportDirectory = lastExportDirectory;
    }

    public static String getLastCommand() {
        return lastCommand;
    }

    public static void setLastCommand(String lastCommand) {
        Config.lastCommand = lastCommand;
    }

    public static ArrayList<String> getHost_history() {
        return host_history;
    }

    public static void addHost_history(String e) {
        host_history.add(e);
    }

    public static int getNumber_host_history() {
        return number_host_history;
    }

    public static void setNumber_host_history(int number_host_history) {
        Config.number_host_history = number_host_history;
    }

    public static Font getDEFAULT_FONT() {
        return DEFAULT_FONT;
    }

    public static int getImageHeight() {
        return ImageHeight;
    }

    public static void setImageHeight(int ImageHeight) {
        Config.ImageHeight = ImageHeight;
    }

    public static int getImageWidth() {
        return ImageWidth;
    }

    public static void setImageWidth(int ImageWidth) {
        Config.ImageWidth = ImageWidth;
    }

    public static String getPDFPageFormat() {
        return PDFPageFormat;
    }

    public static void setPDFPageFormat(String PDFPageFormat) {
        Config.PDFPageFormat = PDFPageFormat;
    }

    

    public static int store_configdir() {
        Properties systemprops = System.getProperties();
        String userhome = (String) systemprops.get("user.home") + systemprops.get("file.separator");
        String username = (String) systemprops.get("user.name");
        String fileseparator = (String) systemprops.get("file.separator");
        // mkdir userhome/.ksar
        String buffer = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n\n<ConfiG>\n</ConfiG>\n";
        boolean home = new File(userhome + ".ksarcfg").mkdir();
        if (!home) {
            return 0;
        }

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(userhome + ".ksarcfg" + fileseparator + "Config.xml"));
            out.write(buffer);
            out.flush();
            out.close();
            return 1;
        } catch (IOException e) {
            return 0;
        }

    }

    public static int getLocal_configfile() {
        return local_configfile;
    }

    public static void setLocal_configfile(int local_configfile) {
        Config.local_configfile = local_configfile;
    }

    public static String getLinuxDateFormat() {
        return LinuxDateFormat;
    }

    public static void setLinuxDateFormat(String LinuxDateFormat) {
        Config.LinuxDateFormat = LinuxDateFormat;
    }

    
    
    private static String landf;
    private static File lastReadDirectory;
    private static File lastExportDirectory;
    private static String lastCommand;
    private static int number_host_history;
    private static int local_configfile;
    private static ArrayList<String> host_history = new ArrayList<String>();
    public static final Font DEFAULT_FONT = new Font("SansSerif", Font.BOLD, 18);

    private static String LinuxDateFormat;
    private static String PDFPageFormat;
    private static int ImageWidth;
    private static int ImageHeight;
    
}
