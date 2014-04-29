package org.helioviewer.plugins.eveplugin.radio.test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

public class TextImageTestPanel extends JPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create the panel.
     */
    public TextImageTestPanel() {
        setLayout(new BorderLayout());
        JPanel temp1 = new JPanel();
        temp1.setLayout(new BorderLayout());
        temp1.setBorder(new TitledBorder("The image"));
        JScrollPane scrollPane = new JScrollPane();
        temp1.add(scrollPane, BorderLayout.CENTER);
        add(temp1, BorderLayout.CENTER);

        JTextArea textArea = new JTextArea();
        textArea.setRows(15);
        JPanel temp = new JPanel();
        temp.setLayout(new BorderLayout());
        temp.setBorder(new TitledBorder("The text"));
        temp.add(new JScrollPane(textArea), BorderLayout.CENTER);
        add(temp, BorderLayout.SOUTH);

    }

    public TextImageTestPanel(StringBuffer text) {
        setLayout(new BorderLayout());
        JPanel temp1 = new JPanel();
        temp1.setLayout(new BorderLayout());
        temp1.setBorder(new TitledBorder("The image"));
        temp1.add(new JLabel("No image"), BorderLayout.CENTER);
        add(temp1, BorderLayout.CENTER);

        JTextArea textArea = new JTextArea();
        textArea.setRows(15);
        JPanel temp = new JPanel();
        temp.setLayout(new BorderLayout());
        temp.setBorder(new TitledBorder("The text"));
        temp.add(new JScrollPane(textArea), BorderLayout.CENTER);
        add(temp, BorderLayout.SOUTH);
        textArea.setText(text.toString());
    }

    public TextImageTestPanel(StringBuffer text, TestRadioPane trp) {
        setLayout(new BorderLayout());
        JPanel temp1 = new JPanel();
        temp1.setLayout(new BorderLayout());
        temp1.setBorder(new TitledBorder("The image"));
        JScrollPane scrollPane = new JScrollPane(trp);
        temp1.add(scrollPane, BorderLayout.CENTER);
        add(temp1, BorderLayout.CENTER);

        JTextArea textArea = new JTextArea();
        textArea.setRows(15);
        JPanel temp = new JPanel();
        temp.setBorder(new TitledBorder("The text"));
        temp.setLayout(new BorderLayout());
        temp.add(new JScrollPane(textArea), BorderLayout.CENTER);
        add(temp, BorderLayout.SOUTH);
        textArea.setText(text.toString());
    }

    public TextImageTestPanel(TestRadioPane trp) {
        setLayout(new BorderLayout());

        JPanel temp1 = new JPanel();
        temp1.setLayout(new BorderLayout());
        temp1.setBorder(new TitledBorder("The image"));
        JScrollPane scrollPane = new JScrollPane(trp);
        temp1.add(scrollPane, BorderLayout.CENTER);
        add(temp1, BorderLayout.CENTER);

        JTextArea textArea = new JTextArea();
        textArea.setRows(15);
        textArea.setText("No text available");
        JPanel temp = new JPanel();
        temp.setLayout(new BorderLayout());
        temp.setBorder(new TitledBorder("The text"));
        temp.add(new JScrollPane(textArea), BorderLayout.CENTER);
        add(temp, BorderLayout.SOUTH);
    }
}
