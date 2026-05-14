package org.helioviewer.jhv.gui;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.helioviewer.jhv.Message;

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

            JOptionPane optionPane = new JOptionPane();
            optionPane.setMessage(scrollPane);
            optionPane.setMessageType(type);
            optionPane.setOptions(new String[]{"Close"});
            optionPane.createDialog(JHVFrame.getFrame(), title).setVisible(true);
        });
    }

}
