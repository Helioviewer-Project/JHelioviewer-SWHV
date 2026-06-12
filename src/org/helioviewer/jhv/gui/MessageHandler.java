package org.helioviewer.jhv.gui;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.helioviewer.jhv.Message;
import org.helioviewer.jhv.gui.components.base.HTMLPane;

final class MessageHandler implements Message.Handler {

    @Override
    public void err(String title, Object msg) {
        show(title, msg, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void warn(String title, Object msg) {
        show(title, msg, JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void fatalErr(String msg) {
        JOptionPane.showMessageDialog(null, Message.format(msg), "Fatal Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void show(String title, Object msg, int type) {
        if (Thread.currentThread().isInterrupted())
            return;

        EventQueue.invokeLater(() -> {
            JTextArea textArea = new JTextArea();
            textArea.setText(Message.format(msg));
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(600, 400));

            HTMLPane report = new HTMLPane();
            report.setOpaque(false);
            report.addHyperlinkListener(DesktopIntegration.hyperOpenURL);
            String url = "https://github.com/Helioviewer-Project/api/issues/new";
            report.setText("If this is a JPIP connection failure, you can open a bug report for the<br>Helioviewer server at <a href='" + url + "'>" + url + "</a>.");

            JOptionPane optionPane = new JOptionPane();
            optionPane.setMessage(new Object[]{report, scrollPane});
            optionPane.setMessageType(type);
            optionPane.setOptions(new String[]{"Close"});
            optionPane.createDialog(JHVFrame.getFrame(), title).setVisible(true);
        });
    }

}
