package org.helioviewer.jhv.gui.dialog;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.TransferAccess;
import org.helioviewer.jhv.gui.component.HTMLPane;
import org.helioviewer.jhv.thread.Task;

public class LogDialog implements Interfaces.ShowableDialog {

    @Override
    public void showDialog() {
        Task.submit("log", Log::get, LogDialog::showDialog, Log::error);
    }

    private static void showDialog(String log) {
        HTMLPane report = new HTMLPane();
        report.setOpaque(false);
        report.addHyperlinkListener(DesktopIntegration.hyperOpenURL);
        report.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));
        report.setText("If you encounter a problem, please attach this log to the problem description and open an issue at <a href='" + JHVGlobals.bugURL + "'>" + JHVGlobals.bugURL + "</a>.<br/>");

        JLabel copyToClipboard = new JLabel("<html><a href=''>Click here to copy the log to the clipboard.");
        copyToClipboard.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                TransferAccess.writeClipboard(log);
                JOptionPane.showMessageDialog(null, "Log copied to clipboard.");
            }
        });

        JTextArea textArea = new JTextArea(log);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane optionPane = new JOptionPane();
        optionPane.setMessage(new Object[]{report, copyToClipboard, new JSeparator(), scrollPane});
        optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
        optionPane.setOptions(new String[]{"Close"});
        optionPane.createDialog(JHVFrame.getFrame(), "JHelioviewer Log").setVisible(true);
    }

}
