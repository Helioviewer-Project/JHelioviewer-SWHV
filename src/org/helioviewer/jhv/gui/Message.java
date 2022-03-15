package org.helioviewer.jhv.gui;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Message {

    private static void show(String title, Object msg, int type) {
        if (Thread.currentThread().isInterrupted())
            return;

        EventQueue.invokeLater(() -> {
            String str = msg == null || msg.toString().isEmpty() ? "No details available." : msg.toString();

            JTextArea textArea = new JTextArea();
            textArea.setText(str);
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

    public static void err(String title, Object msg) {
        show(title, msg, JOptionPane.ERROR_MESSAGE);
    }

    public static void warn(String title, Object msg) {
        show(title, msg, JOptionPane.WARNING_MESSAGE);
    }

    public static void fatalErr(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Fatal Error", JOptionPane.ERROR_MESSAGE);
        System.exit(-1);
    }

}
