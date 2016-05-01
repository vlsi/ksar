/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.atomique.ksar;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import net.atomique.ksar.UI.Desktop;
import net.atomique.ksar.UI.SplashScreen;

/**
 *
 * @author Max
 */
public class Main {

    static Config config = null;
    static GlobalOptions globaloptions = null;
    static ResourceBundle resource = ResourceBundle.getBundle("net/atomique/ksar/Language/Message");

    ;

    public static void usage() {
        show_version();
    }

    public static void show_version() {
        System.err.println("ksar Version : " + VersionNumber.getVersionNumber());
    }

    private static void set_lookandfeel() {
        for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
            if (Config.getLandf().equals(laf.getName())) {
                try {
                    UIManager.setLookAndFeel(laf.getClassName());
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(Desktop.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InstantiationException ex) {
                    Logger.getLogger(Desktop.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(Desktop.class.getName()).log(Level.SEVERE, null, ex);
                } catch (UnsupportedLookAndFeelException ex) {
                    Logger.getLogger(Desktop.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static void make_ui() {
        SplashScreen mysplash = new SplashScreen(null, 3000);
        while (mysplash.isVisible()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }


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
        /// load default
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
            }
        }

        make_ui();



        System.out.println("exit");
    }

    public static void exit_error(final String message) {
        System.err.println(message);
        System.exit(1);

    }
}
