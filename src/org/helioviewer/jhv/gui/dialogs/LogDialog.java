package org.helioviewer.jhv.gui.dialogs;

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
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.JHVTransferHandler;
import org.helioviewer.jhv.gui.components.base.HTMLPane;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

public class LogDialog implements ShowableDialog {

    @Override
    public void showDialog() {
        String log = Log.get();

        HTMLPane report = new HTMLPane();
        report.setOpaque(false);
        report.addHyperlinkListener(JHVGlobals.hyperOpenURL);
        report.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));
        report.setText("If you encounter a problem, please attach this log to the problem description and open an issue at <a href='" + JHVGlobals.bugURL + "'>" + JHVGlobals.bugURL + "</a>.<br/>");

        JLabel copyToClipboard = new JLabel("<html><a href=''>Click here to copy the log to the clipboard.");
        copyToClipboard.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                JHVTransferHandler.getInstance().toClipboard(log);
                JOptionPane.showMessageDialog(null, "Log copied to clipboard.");
            }
        });

        JTextArea textArea = new JTextArea();
        textArea.setText(log);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        Object[] objects = new Object[]{report, copyToClipboard, new JSeparator(), scrollPane};

        JOptionPane optionPane = new JOptionPane();
        optionPane.setMessage(objects);
        optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
        optionPane.setOptions(new String[]{"Close"});
        optionPane.createDialog(JHVFrame.getFrame(), "JHelioviewer Log").setVisible(true);
    }

}
