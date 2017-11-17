/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import net.atomique.ksar.ui.Desktop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.beust.jcommander.JCommander;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  static Config config = null;
  static GlobalOptions globaloptions = null;
  static ResourceBundle resource = ResourceBundle.getBundle("net/atomique/ksar/Language/Message");

  public static void usage() {
    show_version();
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


    //Cmdline_parsing
    CommandLineArgs cmdl_args = new CommandLineArgs();
    JCommander .newBuilder()
        .addObject(cmdl_args)
        .build()
        .parse(args);

    if (cmdl_args.isVersion()) {
      show_version();
      System.exit(0);
    }

    if (cmdl_args.isHelp()) {
      usage();
      System.exit(0);
    }

    if (cmdl_args.isDebug()) {
      GlobalOptions.setDodebug(true);
    }  else {
      GlobalOptions.setDodebug(false);
    }

    if (cmdl_args.getFilename() != null) {
      if (cmdl_args.getFilename().isEmpty()) {
        exit_error(resource.getString("INPUT_REQUIRE_ARG"));
      }
      else {
        GlobalOptions.setCLfilename(cmdl_args.getFilename());
      }
    }
    make_ui();

  }

  public static void exit_error(final String message) {
    log.error(message);
    System.exit(1);

  }
}
