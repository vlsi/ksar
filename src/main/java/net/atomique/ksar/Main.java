/*
 * Copyright 2008 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import ch.qos.logback.classic.Level;
import net.atomique.ksar.ui.Desktop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);
  static {
    ((ch.qos.logback.classic.Logger)log).setLevel(Level.INFO);
  }

  static Config config = null;
  static GlobalOptions globaloptions = null;
  static ResourceBundle resource = ResourceBundle.getBundle("net/atomique/ksar/Language/Message");

  public static void usage() {
    log.info("Usage: ksar [OPTIONS]");
    log.info("OPTIONS:");
    log.info("  -input INPUTFILE    load INPUTFILE sa sar data");
    log.info("  -debug              enable debug level output");
    log.info("  -test               an alist for -debug option");
    log.info("  -trace              enable trace level  output");
    log.info("  -version            print the version information");
    log.info("  -help               print this help message");
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

    javax.swing.SwingUtilities.invokeLater(() -> {
      GlobalOptions.setUI(new Desktop());
      SwingUtilities.updateComponentTreeUI(GlobalOptions.getUI());
      GlobalOptions.getUI().add_window();
      GlobalOptions.getUI().maxall();
    });

  }

  public static void main(String[] args) {
    int i = 0;
    String arg;

    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

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
          System.exit(0);
        }
        if ("-test".equals(arg) || "-debug".equals(arg)) {
          root.setLevel(Level.DEBUG);
          continue;
        }
        if ("-trace".equals(arg)) {
          root.setLevel(Level.TRACE);
          continue;
        }
        if ("-input".equals(arg)) {
          if (i < args.length) {
            GlobalOptions.setCLfilename(args[i++]);
            continue;
          } else {
            exit_error(resource.getString("INPUT_REQUIRE_ARG"));
          }
        }
        exit_error(resource.getString("UNKNOWN_OPTION"), arg);
      }

      }
    }

    make_ui();

  }

  public static void exit_error(final String format, final String... args) {
    log.error(format, (Object[]) args);
    System.exit(1);

  }
}
