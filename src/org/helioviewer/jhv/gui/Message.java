package org.helioviewer.jhv.gui;

import java.awt.EventQueue;

import javax.swing.JOptionPane;

import org.helioviewer.jhv.base.Regex;

public class Message {

    // Format string with at most 70 characters per line
    private static String formatMessage(String message) {
        StringBuilder sb = new StringBuilder();
        int lineLength = 0;
        for (String word : Regex.Space.split(message)) {
            if (lineLength + word.length() < 70) {
                lineLength += word.length() + 1;
            } else {
                sb.append('\n');
                lineLength = word.length() + 1;
            }
            sb.append(word);
            sb.append(' ');
        }
        return sb.toString();
    }

    public static void err(String title, Object msg, boolean exitImmediately) {
        if (Thread.currentThread().isInterrupted())
            return;
        // invoked immediately
        String str = msg == null || msg.toString().isEmpty() ? "No error details available." : formatMessage(msg.toString());
        JOptionPane.showMessageDialog(null, (title == null ? "" : title + '\n') + str, exitImmediately ? "Fatal Error" : "Error", JOptionPane.ERROR_MESSAGE);
        if (exitImmediately)
            System.exit(-1);
    }

    public static void warn(String title, Object msg) {
        if (Thread.currentThread().isInterrupted())
            return;

        String str = msg == null || msg.toString().isEmpty() ? "No warning details available." : formatMessage(msg.toString());
        EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(null, (title == null ? "" : title + '\n') + str, "Warning", JOptionPane.WARNING_MESSAGE));
    }

}
