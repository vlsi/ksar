package net.atomique.ksar.UI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

public class  SplashScreen extends JWindow {

    private static final Logger log = LoggerFactory.getLogger(SplashScreen.class);

    private static final long serialVersionUID = 8L;

    public SplashScreen( Frame f, int waitTime) {
        super(f);

        log.trace("draw SplashScreen");

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(new SplashPicture());
        p.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        getContentPane().add(p);
        setSize(450, 300);
        setLocationRelativeTo(null);
        setVisible(true);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - (450 / 2), screenSize.height / 2 - (300 / 2));
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                setVisible(false);
                dispose();
            }
        });
        final int pause = waitTime;
        final Runnable closerRunner = new Runnable() {

            public void run() {
                setVisible(false);
                dispose();
            }
        };
        Runnable waitRunner = new Runnable() {

            public void run() {
                try {
                    Thread.sleep(pause);
                    SwingUtilities.invokeAndWait(closerRunner);
                } catch (Exception ex) {
                    log.error("SplashScreen run exception",ex);
                }
            }
        };
        setVisible(true);
        Thread splashThread = new Thread(waitRunner, "SplashThread");
        splashThread.start();
    }
}
