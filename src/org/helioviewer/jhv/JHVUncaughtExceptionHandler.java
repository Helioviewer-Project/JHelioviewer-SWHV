package org.helioviewer.jhv;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import org.helioviewer.jhv.gui.JHVTransferHandler;
import org.helioviewer.jhv.gui.components.base.HTMLPane;

class JHVUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

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
                "or use it to open an issue at <a href='" + JHVGlobals.bugURL + "'>" + JHVGlobals.bugURL + "</a>.<br/>");

        JLabel copyToClipboard = new JLabel("<html><a href=''>Click here to copy the error report to the clipboard.");
        copyToClipboard.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                JHVTransferHandler.getInstance().toClipboard(msg);
                JOptionPane.showMessageDialog(null, "Error report copied to clipboard.");
            }
        });

        JTextArea textArea = new JTextArea();
        textArea.setText(msg);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        Object[] objects = new Object[]{report, copyToClipboard, new JSeparator(), scrollPane};

        JOptionPane optionPane = new JOptionPane();
        optionPane.setMessage(objects);
        optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
        optionPane.setOptions(new String[]{"Quit JHelioviewer", "Continue"});
        optionPane.createDialog("JHelioviewer: Fatal Error").setVisible(true);

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
        msg += "Log:\n" + Log.get();

        if (!alreadySent) {
            alreadySent = true;
            String errorMsg = msg;
            EventQueue.invokeLater(() -> showErrorDialog(errorMsg));
        }
    }

}
