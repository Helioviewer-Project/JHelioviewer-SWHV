package org.helioviewer.jhv.gui.dialogs;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class TextDialog extends StandardDialog implements ShowableDialog {

    private final String text;

    public TextDialog(String title, URL textFile) {
        super(ImageViewerGui.getMainFrame(), title, true);
        setResizable(false);

        StringBuilder sb = new StringBuilder();
        try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(textFile.openStream(), StandardCharsets.UTF_8)))) {
            String linebreak = System.getProperty("line.separator");
            while (scanner.hasNext()) {
                sb.append(scanner.nextLine()).append(linebreak);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        text = sb.toString();
    }

    public TextDialog(String title, String _text) {
        super(ImageViewerGui.getMainFrame(), title, true);
        text = _text;
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        setDefaultAction(close);
        setDefaultCancelAction(close);

        JButton button = new JButton(close);
        button.setText("Close");
        setInitFocusedComponent(button);

        ButtonPanel panel = new ButtonPanel();
        panel.add(button);

        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width + 50, 500));

        return scrollPane;
    }

    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

}
