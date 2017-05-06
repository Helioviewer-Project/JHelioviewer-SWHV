package org.helioviewer.jhv;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import org.apache.log4j.NDC;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.gui.ClipBoardCopier;

// Catch and handle all runtime exceptions
class JHVUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final String BUG_URL = "https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues";
    private static final String MAIL_URL = "swhv@sidc.be";

    private static final int default_width = 600;
    private static final int default_height = 400;

    private static final JHVUncaughtExceptionHandler instance = new JHVUncaughtExceptionHandler();

    private JHVUncaughtExceptionHandler() {
        try (InputStream is = JHVUncaughtExceptionHandler.class.getResourceAsStream("/sentry.properties")) {
            Properties p = new Properties();
            p.load(is);
            for (String key : p.stringPropertyNames())
                System.setProperty(key, p.getProperty(key));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method sets the default uncaught exception handler. Thus, this
     * method should be called once when the application starts.
     */
    public static void setupHandlerForThread() {
        Thread.setDefaultUncaughtExceptionHandler(instance);
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
    private static void showErrorDialog(String title, Object msg) {
        ArrayList<Object> objects = new ArrayList<>();

        JLabel fatal = new JLabel("Fatal error detected.");
        objects.add(fatal);

        Font font = fatal.getFont();
        JEditorPane report = new JEditorPane("text/html", "<html><font style=\"font-family: '" + font.getFamily() + "'; font-size: " + font.getSize() + ";\">" +
                                                          "Please email this report at <a href='mailto:" + MAIL_URL + "'>" + MAIL_URL + "</a> " +
                                                          "or use it to open an issue at <a href='" + BUG_URL + "'>" + BUG_URL + "</a> " + "</font></html>");
        report.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));
        report.setEditable(false);
        report.setOpaque(false);
        report.addHyperlinkListener(JHVGlobals.hyperOpenURL);
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

    // we do not use the logger here, since it should work even before logging initialization
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        StringBuilder stackTrace = new StringBuilder();
        stackTrace.append(e.getClass().getCanonicalName()).append('\n');
        for (StackTraceElement el : e.getStackTrace()) {
            stackTrace.append("at ").append(el).append('\n');
        }

        String msg = "";
        StringBuilder sb = new StringBuilder();
        String logName = Log.getCurrentLogFile();
        if (logName != null) {
            Log.error("Runtime exception", e);
            try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(logName), StandardCharsets.UTF_8))) {
                String line;
                while ((line = input.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                NDC.push(sb.toString());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            System.err.println("Runtime exception");
            System.err.println(stackTrace);
            msg += "Uncaught Exception in " + JHVGlobals.userAgent;
            msg += "\nDate: " + new Date();
            msg += "\nThread: " + t;
            msg += "\nMessage: " + e.getMessage();
            msg += "\nStacktrace:\n";
            msg += stackTrace + "\n";
        }

        Log.fatal(null, e);
        showErrorDialog("JHelioviewer: Fatal Error", msg + "Log:\n" + sb.toString());
    }

}
