package org.helioviewer.jhv;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextPane;

import org.apache.log4j.NDC;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.gui.ClipBoardCopier;

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

    private static void showErrorDialog(String msg) {
        ArrayList<Object> objects = new ArrayList<>();

        JTextPane report = new JTextPane();
        report.setContentType("text/html");
        report.setEditable(false);
        report.setOpaque(false);
        report.addHyperlinkListener(JHVGlobals.hyperOpenURL);
        report.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        report.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));
        report.setText("Fatal error detected." +
                       "<p>Please email this report at <a href='mailto:" + MAIL_URL + "'>" + MAIL_URL + "</a> " +
                       "or use it to open an issue at <a href='" + BUG_URL + "'>" + BUG_URL + "</a>.<br/>" +
                       "This report was sent to swhv.oma.be.");
        objects.add(report);

        JLabel copyToClipboard = new JLabel("<html><a href=''>Click here to copy the error report to the clipboard.");
        copyToClipboard.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                ClipBoardCopier.getSingletonInstance().setString(msg);
                JOptionPane.showMessageDialog(null, "Error report copied to clipboard.");
            }
        });

        JTextArea textArea = new JTextArea();
        textArea.setMargin(new Insets(5, 5, 5, 5));
        textArea.setText(msg);
        textArea.setEditable(false);
        JScrollPane sp = new JScrollPane(textArea);
        sp.setPreferredSize(new Dimension(default_width, default_height));

        objects.add(copyToClipboard);
        objects.add(new JSeparator());
        objects.add(sp);

        JOptionPane optionPane = new JOptionPane();
        optionPane.setMessage(objects.toArray());
        optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
        optionPane.setOptions(new String[] { "Quit JHelioviewer", "Continue" });
        JDialog dialog = optionPane.createDialog(null, "JHelioviewer: Fatal Error");

        dialog.setVisible(true);
        if ("Quit JHelioviewer".equals(optionPane.getValue()))
            System.exit(1);
    }

    // we do not use the logger here, since it should work even before logging initialization
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        StringBuilder stackTrace = new StringBuilder(e.getClass().getCanonicalName() + '\n');
        for (StackTraceElement el : e.getStackTrace()) {
            stackTrace.append("at ").append(el).append('\n');
        }

        String msg = "";
        StringBuilder sb = new StringBuilder();

        File logFile;
        String logName = Log.getCurrentLogFile();
        if (logName != null && (logFile = new File(logName)).canRead()) {
            Log.error("Runtime exception", e);
            try (BufferedReader input = Files.newBufferedReader(logFile.toPath(), StandardCharsets.UTF_8)) {
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
        showErrorDialog(msg + "Log:\n" + sb);
    }

}
