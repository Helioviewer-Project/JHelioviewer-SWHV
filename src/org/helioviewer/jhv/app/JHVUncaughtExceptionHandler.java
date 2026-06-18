package org.helioviewer.jhv.app;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.TransferAccess;
import org.helioviewer.jhv.gui.component.HTMLPane;

public final class JHVUncaughtExceptionHandler {

    // This method should be called once when the application starts
    public static void setupHandlerForThread(boolean headless) {
        Thread.setDefaultUncaughtExceptionHandler(headless ? new Headless() : new GUI());
    }

    private abstract static class Base implements Thread.UncaughtExceptionHandler {
        private final AtomicBoolean alreadySent = new AtomicBoolean(false);

        @Override
        public final void uncaughtException(Thread t, Throwable e) {
            if (alreadySent.compareAndSet(false, true))
                handle(format(t, e));
        }

        // Do not use the logger here; this must work even before logging initialization.
        private static String format(Thread t, Throwable e) {
            StringWriter stackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(stackTrace));

            String msg = "";
            msg += "Uncaught Exception in " + AppInfo.userAgent;
            msg += "\nThread: " + t;
            msg += "\nMessage: " + e.getMessage();
            msg += "\nStacktrace:\n";
            msg += stackTrace;
            msg += "Log:\n" + Log.get();
            return msg;
        }

        abstract void handle(String msg);
    }

    private static final class GUI extends Base {
        @Override
        void handle(String msg) {
            EventQueue.invokeLater(() -> showErrorDialog(msg));
        }

        private static void showErrorDialog(String msg) {
            HTMLPane report = new HTMLPane();
            report.setOpaque(false);
            report.addHyperlinkListener(DesktopIntegration.hyperOpenURL);
            report.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));
            report.setText("Fatal error detected." +
                    "<p>Please email this report at <a href='mailto:" + AppInfo.emailAddress + "'>" + AppInfo.emailAddress + "</a> " +
                    "or use it to open an issue at <a href='" + AppInfo.bugURL + "'>" + AppInfo.bugURL + "</a>.<br/>");

            JLabel copyToClipboard = new JLabel("<html><a href=''>Click here to copy the error report to the clipboard.");
            copyToClipboard.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent me) {
                    TransferAccess.writeClipboard(msg);
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
    }

    private static final class Headless extends Base {
        @Override
        void handle(String msg) {
            System.err.println(msg);
            System.exit(1);
        }
    }

    private JHVUncaughtExceptionHandler() {}
}
