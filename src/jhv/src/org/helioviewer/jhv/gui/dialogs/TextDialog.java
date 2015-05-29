package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

@SuppressWarnings({"serial"})
public class TextDialog extends JDialog implements ActionListener, ShowableDialog {

    public TextDialog(String title, URL textFile) {
        super(ImageViewerGui.getMainFrame(), title, true);
        setResizable(false);

        String text = "";
        String linebreak = System.getProperty("line.separator");

        try {
            Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(textFile.openStream(), "UTF-8")));
            while (scanner.hasNext()) {
                text += scanner.nextLine() + linebreak;
            }
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        init(text);
    }

    public TextDialog(String title, String text) {
        super(ImageViewerGui.getMainFrame(), title, true);
        init(text);
    }

    private void init(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width + 50, 500));
        add(scrollPane, BorderLayout.NORTH);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(this);
        add(closeButton, BorderLayout.EAST);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.dispose();
    }

    @Override
    public void showDialog() {
        pack();
        setSize(getPreferredSize());
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    @Override
    public void init() {
    }

}
