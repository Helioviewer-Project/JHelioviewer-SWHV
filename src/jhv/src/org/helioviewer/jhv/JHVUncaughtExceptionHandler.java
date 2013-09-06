package org.helioviewer.jhv;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.jhv.gui.ClipBoardCopier;

/**
 * Routines to catch and handle all runtime exceptions.
 * 
 * @author Malte Nuhn
 */
public class JHVUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    /**
     * Default size of the error dialog.
     */
    private final static int default_width = 600;
    private final static int default_height = 400;

    private static final JHVUncaughtExceptionHandler handler = new JHVUncaughtExceptionHandler();

    public static JHVUncaughtExceptionHandler getSingletonInstance() {
        return handler;
    }

    /**
     * This method sets the default uncaught exception handler. Thus, this
     * method should be called once when the application starts.
     */
    public static void setupHandlerForThread() {
        Thread.setDefaultUncaughtExceptionHandler(JHVUncaughtExceptionHandler.getSingletonInstance());
    }

    /**
     * Generates a simple error Dialog, allowing the user to copy the
     * errormessage to the clipboard.
     * <p>
     * As options it will show {"Quit JHelioviewer", "Continue"} and quit if
     * necessary.
     * 
     * @param title
     *            Title of the Dialog
     * @param msg
     *            Object to display in the main area of the dialog.
     */
    public static void showErrorDialog(final String title, final Object msg) {

        Vector<Object> objects = new Vector<Object>();
        objects.add(new JLabel("Fatal error detected."));
        objects.add(new JLabel("Please be so kind to report this as a bug on"));
        JLabel bugLabel = new JLabel("https://bugs.launchpad.net/jhelioviewer/+filebug");
        Font font = bugLabel.getFont();
        font = font.deriveFont(font.getStyle() ^ Font.ITALIC);
        bugLabel.setFont(font);
        bugLabel.setForeground(Color.blue);

        objects.add(bugLabel);

        bugLabel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JHVGlobals.openURL("https://bugs.launchpad.net/jhelioviewer/+filebug");
            }
        });

        if (msg instanceof String) {
            JLabel copyToClipboard = new JLabel("Click here to copy the error message to the clipboard.");

            copyToClipboard.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent me) {
                    ClipBoardCopier.getSingletonInstance().setString((String) msg);
                    JOptionPane.showMessageDialog(null, "Copied error message to clipboard.");
                }
            });

            font = copyToClipboard.getFont();
            font = font.deriveFont(font.getStyle() ^ Font.ITALIC);
            copyToClipboard.setFont(font);
            copyToClipboard.setForeground(Color.blue);

            // copyToClipboard.addMouseListener(l);

            String text = (String) msg;

            JTextArea textArea = new JTextArea();
            textArea.setMargin(new Insets(5, 5, 5, 5));
            textArea.setText(text);
            textArea.setEditable(false);
            JScrollPane sp = new JScrollPane(textArea);
            sp.setPreferredSize(new Dimension(default_width, default_height));

            objects.add(new JSeparator());
            objects.add(sp);
            objects.add(copyToClipboard);
        } else {
            objects.add(new JSeparator());
            objects.add(msg);
        }

        JOptionPane optionPane = new JOptionPane(title);
        optionPane.setMessage(objects.toArray());
        optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
        optionPane.setOptions(new String[] { "Quit JHelioviewer", "Continue" });
        JDialog dialog = optionPane.createDialog(null, title);

        dialog.setVisible(true);
        if ("Quit JHelioviewer".equals(optionPane.getValue()))
            System.exit(1);
    }

    private JHVUncaughtExceptionHandler() {
    }

    // we do not use the logger here, since it should work even before logging
    // initialization
    public void uncaughtException(Thread t, Throwable e) {

        String stackTrace = e.getClass().getCanonicalName() + "\n";
        for (StackTraceElement el : e.getStackTrace()) {
            stackTrace = stackTrace + "at " + el.toString() + "\n";
        }

        String msg = "Uncaught Exception detected.\n\nConfiguration:\n";
        msg += "JHelioviewer - Version: " + JHVGlobals.getJhvVersion() + "\n";
        msg += "JHelioviewer - Revision: " + JHVGlobals.getJhvRevision() + "\n";
        msg += "Java Virtual Machine - Name: " + System.getProperty("java.vm.name") + "\n";
        msg += "Java Virtual Machine - Vendor: " + System.getProperty("java.vm.vendor") + "\n";
        msg += "Java Virtual Machine - Version: " + System.getProperty("java.vm.version") + "\n";
        msg += "JRE Specification - Version: " + System.getProperty("java.specification.version") + "\n";
        msg += "Operating System - Name: " + System.getProperty("os.name") + "\n";
        msg += "Operating System - Architecture: " + System.getProperty("os.arch") + "\n";
        msg += "Operating System - Version: " + System.getProperty("os.version") + "\n\n";

        msg += "Date: " + new Date() + "\n";
        msg += "Thread: " + t + "\n";
        msg += "Message: " + e.getMessage() + "\n\n";
        msg += "Stacktrace:\n";
        msg += stackTrace;

        if (LogSettings.getSingletonInstance() != null) {
            Log.fatal("Runtime exception", e);

            msg += "\nLog:\n";
            LogSettings.getSingletonInstance().getCurrentLogFile();

            try {
                BufferedReader input = new BufferedReader(new FileReader(LogSettings.getSingletonInstance().getCurrentLogFile()));
                try {
                    String line = null; // not declared within while loop

                    while ((line = input.readLine()) != null) {
                        msg += line + "\n";
                    }
                } finally {
                    input.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else {
            System.err.println("Runtime exception");
            System.err.println(stackTrace);
        }

        JHVUncaughtExceptionHandler.showErrorDialog("JHelioviewer: Fatal Error", msg);
    }
}
