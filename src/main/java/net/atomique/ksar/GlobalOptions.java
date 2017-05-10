/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import net.atomique.ksar.UI.Desktop;
import net.atomique.ksar.XML.CnxHistory;
import net.atomique.ksar.XML.ColumnConfig;
import net.atomique.ksar.XML.HostInfo;
import net.atomique.ksar.XML.OSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

public class GlobalOptions {

  private static final Logger log = LoggerFactory.getLogger(GlobalOptions.class);

  private static GlobalOptions instance = new GlobalOptions();

  static GlobalOptions getInstance() {
    return instance;
  }

  public static boolean hasUI() {
    return (UI != null);
  }

  private GlobalOptions() {

    log.trace("load GlobalOptions");

    String[] OSParserNames = {"AIX", "HPUX", "Linux", "SunOS"};
    String filename;
    InputStream is;
    XMLConfig tmp;
    systemprops = System.getProperties();
    username = (String) systemprops.get("user.name");
    userhome = (String) systemprops.get("user.home") + systemprops.get("file.separator");
    fileseparator = (String) systemprops.get("file.separator");
    columnlist = new HashMap<>();
    OSlist = new HashMap<>();
    ParserMap = new HashMap<>();
    HistoryList = new HashMap<>();
    HostInfoList = new HashMap<>();
    is = this.getClass().getResourceAsStream("/Config.xml");
    tmp = new XMLConfig(is);
    for (String OSName : OSParserNames) {
      try {
        Class tmpclass = Class.forName("net.atomique.ksar.Parser." + OSName);
        ParserMap.put(OSName, tmpclass);
      } catch (ClassNotFoundException ex) {
        log.error("Parser class not found", ex);
      }

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

  static HashMap<String, ColumnConfig> getColorlist() {
    return columnlist;
  }

  static HashMap<String, OSConfig> getOSlist() {
    return OSlist;
  }

  public static ColumnConfig getColumnConfig(String s) {
    if (columnlist.isEmpty()) {
      return null;
    }
    return columnlist.get(s);
  }

  public static Color getDataColor(String s) {
    ColumnConfig tmp = columnlist.get(s);
    if (tmp != null) {
      return tmp.getData_color();
    } else {
      log.warn("color not found for tag {}", s);
    }
    return null;
  }

  static OSConfig getOSinfo(String s) {
    return OSlist.get(s);
  }

  public static boolean isDodebug() {
    return dodebug;
  }

  static void setDodebug(boolean do_debug) {
    GlobalOptions.dodebug = do_debug;
  }

  static String getCLfilename() {
    return CLfilename;
  }

  static void setCLfilename(String CL_filename) {
    GlobalOptions.CLfilename = CL_filename;
  }

  public static String getFileseparator() {
    return fileseparator;
  }

  static Class getParser(String s) {
    String tmp = s.replaceAll("-", "");
    if (ParserMap.isEmpty()) {
      return null;
    }
    return ParserMap.get(tmp);
  }

  static HashMap<String, HostInfo> getHostInfoList() {
    return HostInfoList;
  }

  public static HostInfo getHostInfo(String s) {
    if (HostInfoList.isEmpty()) {
      return null;
    }
    return HostInfoList.get(s);
  }

  public static void addHostInfo(HostInfo s) {
    HostInfoList.put(s.getHostname(), s);
    saveHistory();
  }

  static HashMap<String, CnxHistory> getHistoryList() {
    return HistoryList;
  }

  static CnxHistory getHistory(String s) {
    if (HistoryList.isEmpty()) {
      return null;
    }
    return HistoryList.get(s);
  }

  static void addHistory(CnxHistory s) {
    CnxHistory tmp = HistoryList.get(s.getLink());
    if (tmp != null) {
      Iterator<String> ite = s.getCommandList().iterator();
      while (ite.hasNext()) {
        tmp.addCommand(ite.next());
      }
    } else {
      HistoryList.put(s.getLink(), s);
    }
    saveHistory();
  }


  static void saveHistory() {
    File tmpfile = null;
    BufferedWriter tmpfile_out = null;

    if (HistoryList.isEmpty() && HostInfoList.isEmpty()) {
      log.info("list is null");
      return;
    }

    try {
      tmpfile = new File(userhome + ".ksarcfg" + fileseparator + "History.xmltemp");

      if (tmpfile.exists()) {
        tmpfile.delete();
      }
      if (tmpfile.createNewFile() && tmpfile.canWrite()) {
        tmpfile_out = new BufferedWriter(new FileWriter(tmpfile));
      }
      //xml header
      tmpfile_out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<ConfiG>\n");
      tmpfile_out.write("\t<History>\n");
      Iterator<String> ite = HistoryList.keySet().iterator();
      while (ite.hasNext()) {
        CnxHistory tmp = HistoryList.get(ite.next());
        tmpfile_out.write(tmp.save());
      }
      //xml footer
      tmpfile_out.write("\t</History>\n");
      tmpfile_out.write("\t<HostInfo>\n");
      Iterator<String> ite2 = HostInfoList.keySet().iterator();
      while (ite2.hasNext()) {
        HostInfo tmp = HostInfoList.get(ite2.next());
        tmpfile_out.write(tmp.save());
      }
      //xml footer
      tmpfile_out.write("\t</HostInfo>\n");
      tmpfile_out.write("</ConfiG>\n");
      tmpfile_out.flush();
      tmpfile_out.close();
      File oldfile = new File(userhome + ".ksarcfg" + fileseparator + "History.xml");
      oldfile.delete();
      tmpfile.renameTo(oldfile);

    } catch (IOException ex) {
      log.error("IO exception", ex);
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
  private static HashMap<String, HostInfo> HostInfoList;
  private static boolean dodebug = false;
  private static String CLfilename = null;
  private static HashMap<String, Class> ParserMap;
  private static boolean firstrun = true;
}
