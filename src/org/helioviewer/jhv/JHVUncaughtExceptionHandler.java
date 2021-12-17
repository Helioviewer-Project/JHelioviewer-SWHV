package org.helioviewer.jhv;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import org.helioviewer.jhv.gui.ClipBoardCopier;
import org.helioviewer.jhv.gui.components.base.HTMLPane;

class JHVUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final String BUG_URL = "https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues";

    private static final JHVUncaughtExceptionHandler instance = new JHVUncaughtExceptionHandler();

    private JHVUncaughtExceptionHandler() {
    }

    // This method should be called once when the application starts
    public static void setupHandlerForThread() {
        Thread.setDefaultUncaughtExceptionHandler(instance);
    }

    private static void showErrorDialog(String msg) {
        HTMLPane report = new HTMLPane();
        report.setOpaque(false);
        report.addHyperlinkListener(JHVGlobals.hyperOpenURL);
        report.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));
        report.setText("Fatal error detected." +
                "<p>Please email this report at <a href='mailto:" + JHVGlobals.emailAddress + "'>" + JHVGlobals.emailAddress + "</a> " +
                "or use it to open an issue at <a href='" + BUG_URL + "'>" + BUG_URL + "</a>.<br/>");

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
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        Object[] objects = new Object[]{report, copyToClipboard, new JSeparator(), scrollPane};

        JOptionPane optionPane = new JOptionPane();
        optionPane.setMessage(objects);
        optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
        optionPane.setOptions(new String[]{"Quit JHelioviewer", "Continue"});
        JDialog dialog = optionPane.createDialog(null, "JHelioviewer: Fatal Error");

        dialog.setVisible(true);
        if ("Quit JHelioviewer".equals(optionPane.getValue()))
            System.exit(1);
    }

    private boolean alreadySent;

    // we do not use the logger here, since it should work even before logging initialization
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        String className = e.getClass().getCanonicalName() + '\n';
        StringBuilder stackTrace = new StringBuilder(className);
        for (StackTraceElement el : e.getStackTrace()) {
            stackTrace.append("at ").append(el).append('\n');
        }

        String msg = "";
        msg += "Uncaught Exception in " + JHVGlobals.userAgent;
        msg += "\nThread: " + t;
        msg += "\nMessage: " + e.getMessage();
        msg += "\nStacktrace:\n";
        msg += stackTrace + "\n";

        try {
            msg += "Log:\n" + Files.readString(Path.of(Log2.getLogFilename()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (!alreadySent) {
            alreadySent = true;
            String errorMsg = msg;
            EventQueue.invokeLater(() -> showErrorDialog(errorMsg));
        }
    }

}
