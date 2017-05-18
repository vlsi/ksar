/*
 * Copyright 2008 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import net.atomique.ksar.export.FileCSV;
import net.atomique.ksar.export.FilePDF;
import net.atomique.ksar.export.FilePNG;
import net.atomique.ksar.graph.Graph;
import net.atomique.ksar.graph.List;
import net.atomique.ksar.ui.Desktop;
import net.atomique.ksar.ui.ParentNodeInfo;
import net.atomique.ksar.ui.SortedTreeNode;
import net.atomique.ksar.ui.TreeNodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);
  private static boolean gui = true;
  static Config config = null;
  static GlobalOptions globaloptions = null;
  static ResourceBundle resource = ResourceBundle.getBundle("net/atomique/ksar/Language/Message");

  public static void usage() {
    log.info("-input : input sar statistics file \n");
    log.info("-n : toggles nongui mode for exporting\n");
    log.info("-outputCSV : location of parsed CSV file\n");
    log.info("-outputPDF : location of output pdf file\n");
    log.info("-outputIMG : prefix for output images\n");
    log.info("-width : width for output png charts\n");
    log.info("-heigth : heigth for output png charts");
    log.info("-tags : comma separated list of nodes for export. ex: 'CPU all,Load'");
    log.info("-dateFormat : date time format. Example: MM/DD/YYYY 23:59:59");
    System.exit(0);
  }

  public static void show_version() {
    log.info("ksar Version : {}", VersionNumber.getVersionNumber());
  }

  private static void set_lookandfeel() {
    for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
      if (Config.getLandf().equals(laf.getName())) {
        try {
          UIManager.setLookAndFeel(laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
          log.error("lookandfeel Exception", ex);
        }
      }
    }
  }

  public static void make_ui() {

    log.trace("MainScreen");
    set_lookandfeel();

    javax.swing.SwingUtilities.invokeLater(new Runnable() {

      public void run() {
        GlobalOptions.setUI(new Desktop());
        SwingUtilities.updateComponentTreeUI(GlobalOptions.getUI());
        GlobalOptions.getUI().add_window();
        GlobalOptions.getUI().maxall();
      }
    });

  }

  public static void main(String[] args) {
    int i = 0;
    String arg;

    log.trace("main - Start");
    log.trace("ksar Version : {}", VersionNumber.getVersionNumber());

    /// load default - Mac OS X Application Properties
    String mrjVersion = System.getProperty("mrj.version");
    if (mrjVersion != null) {
      System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
      System.setProperty("com.apple.mrj.application.apple.menu.about.name", "kSar");
      System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    config = Config.getInstance();
    globaloptions = GlobalOptions.getInstance();

    if (args.length > 0) {
      while (i < args.length && args[i].startsWith("-")) {
        arg = args[i++];
        if ("-version".equals(arg)) {
          show_version();
          System.exit(0);
        }
        if ("-help".equals(arg)) {
          usage();
          continue;
        }
        if ("-test".equals(arg)) {
          GlobalOptions.setDodebug(true);
          continue;
        }
        if ("-input".equals(arg)) {
          if (i < args.length) {
            GlobalOptions.setCLfilename(args[i++]);
          } else {
            exit_error(resource.getString("INPUT_REQUIRE_ARG"));
          }
          continue;
        }
        if ("-outputCSV".equals(arg)) {
          // This will be CSV file to export by deafault
          if (i < args.length) {
            GlobalOptions.setOutCSV(args[i++]);
          } else {
            exit_error(resource.getString("INPUT_REQUIRE_ARG"));
          }
          continue;
        }

        if ("-outputPDF".equals(arg)) {
          // This will be CSV file to export by deafault
          if (i < args.length) {
            GlobalOptions.setOutPDF(args[i++]);
          } else {
            exit_error(resource.getString("INPUT_REQUIRE_ARG"));
          }
          continue;
        }

        // prefix for chart image files
        if ("-outputIMG".equals(arg)) {
          // This will be CSV file to export by deafault
          if (i < args.length) {
            GlobalOptions.setOutIMG(args[i++]);
          } else {
            exit_error(resource.getString("INPUT_REQUIRE_ARG"));
          }
          continue;
        }

        // node names for export comma separated
        if ("-tags".equals(arg)) {
          // This will be CSV file to export by deafault
          if (i < args.length) {
            GlobalOptions.setOutTags(args[i++]);
          } else {
            exit_error(resource.getString("INPUT_REQUIRE_ARG"));
          }
          continue;
        }

        if ("-width".equals(arg)) {
          // This will be CSV file to export by deafault
          if (i < args.length) {
            GlobalOptions.setWidth(Integer.parseInt(args[i++]));
          } else {
            exit_error(resource.getString("INPUT_REQUIRE_ARG"));
          }
          continue;
        }

        if ("-heigth".equals(arg)) {
          // This will be CSV file to export by deafault
          if (i < args.length) {
            GlobalOptions.setWidth(Integer.parseInt(args[i++]));
          } else {
            exit_error(resource.getString("INPUT_REQUIRE_ARG"));
          }
          continue;
        }

        if ("-dateFormat".equals(arg)) {
          // This will be CSV file to export by deafault
          if (i < args.length) {
            Config.setLinuxDateFormat(args[i++]);
            Config.save();
          } else {
            exit_error(resource.getString("INPUT_REQUIRE_ARG"));
          }
          continue;
        }

        if ("-n".equals(arg)) {
          // This means nongui mode (same as in jmeter)
          gui = false;
          continue;
        }

      }
    }

    if (gui) {
      make_ui();
    } else {
      nongui();
    }

  }

  public static void exit_error(final String message) {
    log.error(message);
    System.exit(1);
  }

  public static void validateTags(SortedTreeNode node) {
    int num = node.getChildCount();
    if (num > 0) {
      Object obj1 = node.getUserObject();
      if (obj1 instanceof ParentNodeInfo) {
        ParentNodeInfo tmpnode = (ParentNodeInfo) obj1;
        List nodeobj = tmpnode.getNode_object();
      }
      for (int i = 0; i < num; i++) {
        SortedTreeNode l = (SortedTreeNode) node.getChildAt(i);
        validateTags(l);
      }
    } else {
      Object obj1 = node.getUserObject();
      if (obj1 instanceof TreeNodeInfo) {
        TreeNodeInfo tmpnode = (TreeNodeInfo) obj1;
        Graph nodeobj = tmpnode.getNode_object();
        nodeobj.printSelected = false;
        for (String nodename : GlobalOptions.getOutTags().split(",")) {
          if (nodeobj.getTitle().equals(nodename)) {
            nodeobj.printSelected = true;
          }
        }
      }
    }
  }

  public static void nongui() {
    kSar ks = new kSar();

    ks.do_fileread(GlobalOptions.getCLfilename());
    while (ks.launched_action.isAlive()) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    // filter out graph nodes for given tags
    if (GlobalOptions.getOutTags() != null) {
      validateTags(ks.graphtree);
    }

    log.trace("File parsing completed. Starting export");
    if (GlobalOptions.getOutCSV() != null) {
      Runnable t = new FileCSV(GlobalOptions.getOutCSV(), ks);
      Thread th = new Thread(t);
      th.start();
      while (th.isAlive()) {
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      log.trace("CSV Export completed");
    }

    if (GlobalOptions.getOutPDF() != null) {
      Runnable t = new FilePDF(GlobalOptions.getOutPDF(), ks);
      Thread th = new Thread(t);
      th.start();
      while (th.isAlive()) {
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      log.trace("PDF Export completed");
    }
    if (GlobalOptions.getOutIMG() != null) {
      FilePNG.drawCharts(ks.graphtree, ks);
      log.trace("IMG Export completed");
    }

  }
}
