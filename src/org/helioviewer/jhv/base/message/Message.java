package org.helioviewer.jhv.base.message;

import java.awt.EventQueue;

import javax.swing.JOptionPane;

/**
 * General messages pulled from JHVGlobals
 * 
 * @author caplins
 * @author Helge Dietert
 */
public class Message {
    /**
     * Inserts linebreaks into the message so that it can shown as a message
     * 
     * @param message
     * @return Formatted string with every line at most 70 chararcters
     */
    public static String formatMessageString(String message) {
        String[] messageWords = message.split(" ");
        StringBuilder sb = new StringBuilder();
        int lineLength = 0;
        for (String word : messageWords) {
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

    /**
     * A central error handler. Displays an error message in a JOptionDialog and
     * exits if the flag is set.
     * 
     * @param _title
     *            title of the error message.
     * @param _msg
     *            the message which has to be displayed.
     * @param _exitImmediately
     *            the program exits when the value true will be passed.
     */
    public static void err(String _title, Object _msg, boolean _exitImmediately) {
        if (Thread.currentThread().isInterrupted())
            return;

        EventQueue.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, ((_title == null ? "" : _title + "\n") + (_msg == null ? "No error details available." : _msg.toString())), (_exitImmediately ? "Fatal Error" : "Error"), JOptionPane.ERROR_MESSAGE);
            if (_exitImmediately)
                System.exit(-1);
        });
    }

    /**
     * A central warning handler. Displays a warning message in a JOptionDialog.
     * 
     * @param _title
     *            title of the warning message.
     * @param _msg
     *            the message which has to be displayed.
     */
    public static void warn(String _title, Object _msg) {
        if (Thread.currentThread().isInterrupted())
            return;

        String msg = _msg.toString();
        EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(null, ((_title == null ? "" : _title + "\n") + (msg == null || msg.isEmpty() ? "No warning details available." : msg)), "Warning", JOptionPane.WARNING_MESSAGE));
    }

}
