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
import java.util.ArrayList;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.logging.LogSettings;
import org.helioviewer.jhv.gui.ClipBoardCopier;

/**
 * Routines to catch and handle all runtime exceptions.
 *
 * @author Malte Nuhn
 */
public class JHVUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler, HyperlinkListener {

    private static final String BUG_URL = "https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues";
    private static final String MAIL_URL = "swhv@sidc.be";

    private static final int default_width = 600;
    private static final int default_height = 400;

    private static final JHVUncaughtExceptionHandler handler = new JHVUncaughtExceptionHandler();

    private JHVUncaughtExceptionHandler() {
    }

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
    private static void showErrorDialog(final String title, final Object msg) {
        ArrayList<Object> objects = new ArrayList<Object>();

        JLabel fatal = new JLabel("Fatal error detected.");
        objects.add(fatal);

        Font font = fatal.getFont();
        JEditorPane report = new JEditorPane("text/html", "<html><font style=\"font-family: '" + font.getFamily() + "'; font-size: " + font.getSize() + ";\">" +
                                                          "Please email this report at <a href='mailto:" + MAIL_URL + "'>" + MAIL_URL + "</a> " +
                                                          "or use it to open an issue at <a href='" + BUG_URL + "'>" + BUG_URL + "</a> " + "</font></html>");
        report.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));
        report.setEditable(false);
        report.setOpaque(false);
        report.addHyperlinkListener(handler);
        objects.add(report);

        if (msg instanceof String) {
            JLabel copyToClipboard = new JLabel("Click here to copy the error message to the clipboard.");

            copyToClipboard.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent me) {
                    ClipBoardCopier.getSingletonInstance().setString((String) msg);
                    JOptionPane.showMessageDialog(null, "Error message copied to clipboard.");
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

            objects.add(copyToClipboard);
            objects.add(new JSeparator());
            objects.add(sp);
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

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            JHVGlobals.openURL(e.getURL().toString());
        }
    }

    // we do not use the logger here, since it should work even before logging
    // initialization
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        StringBuilder stackTrace = new StringBuilder();
        stackTrace.append(e.getClass().getCanonicalName()).append("\n");
        for (StackTraceElement el : e.getStackTrace()) {
            stackTrace.append("at ").append(el).append("\n");
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

        showErrorDialog("JHelioviewer: Fatal Error", msg);
    }

}
