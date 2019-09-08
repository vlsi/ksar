/*
 * Copyright 2008 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import net.atomique.ksar.ui.Desktop;
import org.apache.logging.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  static Config config = null;
  static GlobalOptions globaloptions = null;
  static ResourceBundle resource = ResourceBundle.getBundle("net/atomique/ksar/Language/Message");

  public static void usage() {
    show_version();
  }

  public static void show_version() {
    log.info("ksar Version : {}", VersionNumber.getVersionString());
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

    log.info("ksar Version : {}", VersionNumber.getVersionString());
    log.info("Java runtime Version : {}", System.getProperty("java.runtime.version"));
    log.info("Java runtime architecture : {}", System.getProperty("os.arch"));

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
        if ("-test".equals(arg) || "-debug".equals(arg)) {
          GlobalOptions.setDodebug(true);
          org.apache.logging.log4j.core.config.Configurator.setRootLevel(Level.DEBUG);
          continue;
        }
        if ("-trace".equals(arg)) {
          GlobalOptions.setDodebug(true);
          org.apache.logging.log4j.core.config.Configurator.setRootLevel(Level.TRACE);
          continue;
        }
        if ("-input".equals(arg)) {
          if (i < args.length) {
            GlobalOptions.setCLfilename(args[i++]);
          } else {
            exit_error(resource.getString("INPUT_REQUIRE_ARG"));
          }
        }

      }
    }

    make_ui();

  }

  public static void exit_error(final String message) {
    log.error(message);
    System.exit(1);

  }
}
