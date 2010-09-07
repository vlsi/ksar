/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.atomique.ksar;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.atomique.ksar.UI.Desktop;
import net.atomique.ksar.XML.CnxHistory;
import net.atomique.ksar.XML.ColumnConfig;
import net.atomique.ksar.XML.OSConfig;

/**
 *
 * @author Max
 */
public class GlobalOptions {

    private static GlobalOptions instance = new GlobalOptions();

    public static GlobalOptions getInstance() {
        return instance;
    }

    public static boolean hasUI() {
        if (UI != null) {
            return true;
        }
        return false;
    }

    GlobalOptions() {
        String filename = null;
        InputStream is = null;
        XMLConfig tmp = null;
        systemprops = System.getProperties();
        username = (String) systemprops.get("user.name");
        userhome = (String) systemprops.get("user.home") + systemprops.get("file.separator");
        fileseparator = (String) systemprops.get("file.separator");
        columnlist = new HashMap<String, ColumnConfig>();
        OSlist = new HashMap<String, OSConfig>();
        ParserMap = new HashMap<String, Class>();
        HistoryList = new HashMap<String, CnxHistory>();
        is = this.getClass().getResourceAsStream("/Config.xml");
        tmp = new XMLConfig(is);
        try {
            Class[] parserlist = getClasses("net.atomique.ksar.Parser");
            for (int i = 0; i < parserlist.length; i++) {
                String simplename = parserlist[i].getName().replaceFirst("net.atomique.ksar.Parser.", "").replaceAll("-", "");
                ParserMap.put(simplename, parserlist[i]);
            }
        } catch (ClassNotFoundException ex) {
            System.err.println("oups no parser in jar");
        }
        for (String parsername : ParserMap.keySet()) {
            is = this.getClass().getResourceAsStream("/" + parsername + ".xml");
            if (is != null) {
                tmp.load_config(is);
            }
        }

        filename = userhome + ".ksarcfg" + fileseparator + "Config.xml";
        if (new File(filename).canRead()) {
            tmp.load_config(filename);
        }
        filename = userhome + ".ksarcfg" + fileseparator + "History.xml";
        if (new File(filename).canRead()) {
            tmp.load_config(filename);
        }

    }

    public static Desktop getUI() {
        return UI;
    }

    public static void setUI(Desktop UI) {
        GlobalOptions.UI = UI;
    }

    public static String getUserhome() {
        return userhome;
    }

    public static String getUsername() {
        return username;
    }

    public static HashMap<String, ColumnConfig> getColorlist() {
        return columnlist;
    }

    public static HashMap<String, OSConfig> getOSlist() {
        return OSlist;
    }

    public static ColumnConfig getColumnConfig(String s) {
        if ( columnlist.isEmpty()) {
            return null;
        }
        return columnlist.get(s);
    }
    public static Color getDataColor(String s) {
        ColumnConfig tmp = columnlist.get(s);
        if (tmp != null) {
            return tmp.getData_color();
        } else {
            System.err.println("WARN: color not found for tag " + s);
        }
        return null;
    }

    public static OSConfig getOSinfo(String s) {
        return OSlist.get(s);
    }

    public static boolean isDodebug() {
        return dodebug;
    }

    public static void setDodebug(boolean do_debug) {
        GlobalOptions.dodebug = do_debug;
    }

    public static String getCLfilename() {
        return CLfilename;
    }

    public static void setCLfilename(String CL_filename) {
        GlobalOptions.CLfilename = CL_filename;
    }

    public static String getFileseparator() {
        return fileseparator;
    }

    public static Class getParser(String s) {
        String tmp = s.replaceAll("-", "");
        if (ParserMap.isEmpty()) {
            return null;
        }
        return ParserMap.get(tmp);
    }

    public static HashMap<String, CnxHistory> getHistoryList() {
        return HistoryList;
    }

    public static CnxHistory getHistory(String s) {
        if (HistoryList.isEmpty()) {
            return null;
        }
        return HistoryList.get(s);
    }

    public static void addHistory(CnxHistory s) {
        CnxHistory tmp = HistoryList.get(s.getLink());
        if ( tmp != null) {
            Iterator<String> ite = s.getCommandList().iterator();
            while (ite.hasNext()) {
                tmp.addCommand(ite.next());
            }
        } else {
            HistoryList.put(s.getLink(), s);
        }
        saveHistory();
    }

    /*
    http://forums.sun.com/thread.jspa?threadID=341935&tstart=0kage
    @throws ClassNotFoundException if the Package is invalid
     */
    public static Class[] getClasses(String pckgname) throws ClassNotFoundException {
        ArrayList<Class> classes = new ArrayList<Class>();
        // Get a File object for the package
        File directory = null;
        try {
            ClassLoader cld = Thread.currentThread().getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException("Can't get class loader.");
            }
            String path = pckgname.replace('.', '/');
            URL resource = cld.getResource(path);
            if (resource == null) {
                throw new ClassNotFoundException("No resource for " + path);
            }
            directory = new File(resource.getFile());
        } catch (NullPointerException x) {
            throw new ClassNotFoundException(pckgname + " (" + directory + ") does not appear to be a valid package");
        }
        if (directory.exists()) {
            // Get the list of the files contained in the package
            String[] files = directory.list();
            for (int i = 0; i < files.length; i++) {
                // we are only interested in .class files
                if (files[i].endsWith(".class")) {
                    // removes the .class extension
                    classes.add(Class.forName(pckgname + '.' + files[i].substring(0, files[i].length() - 6)));
                }
            }
        } else {
            throw new ClassNotFoundException(pckgname + " does not appear to be a valid package");
        }
        Class[] classesA = new Class[classes.size()];
        classes.toArray(classesA);
        return classesA;
    }

    public static void saveHistory() {
        File tmpfile = null;
        BufferedWriter tmpfile_out = null;

        if (HistoryList.isEmpty()) {
            return;
        }

        try {
            tmpfile = new File(userhome + ".ksarcfg" + fileseparator + "History.xmltemp");

            if ( tmpfile.exists() ) {
                tmpfile.delete();
            }
            if (tmpfile.createNewFile() && tmpfile.canWrite()) {
                tmpfile_out = new BufferedWriter(new FileWriter(tmpfile));
            }
            //xml header
            tmpfile_out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<ConfiG>\n\t<History>\n");
            Iterator<String> ite = HistoryList.keySet().iterator();
            while (ite.hasNext()) {
                CnxHistory tmp = HistoryList.get(ite.next());
                tmpfile_out.write(tmp.save());
            }
            //xml footer
            tmpfile_out.write("\t</History>\n</ConfiG>\n");
            tmpfile_out.flush();
            tmpfile_out.close();
            tmpfile.renameTo(new File(userhome + ".ksarcfg" + fileseparator + "History.xml"));
        } catch (IOException ex) {
            Logger.getLogger(GlobalOptions.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    private static Desktop UI = null;
    private static Properties systemprops;
    private static String userhome;
    private static String username;
    private static String fileseparator;
    private static HashMap<String, ColumnConfig> columnlist;
    private static HashMap<String, OSConfig> OSlist;
    private static HashMap<String, CnxHistory> HistoryList;
    private static boolean dodebug = false;
    private static String CLfilename = null;
    private static HashMap<String, Class> ParserMap;
    private static boolean firstrun = true;
}
