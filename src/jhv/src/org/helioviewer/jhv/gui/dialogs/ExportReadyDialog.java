package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

@SuppressWarnings("serial")
public class ExportReadyDialog extends JDialog implements ShowableDialog {

    private final JTextPane messagePane = new JTextPane();

    public ExportReadyDialog() {
        super(ImageViewerGui.getMainFrame(), false);
        setLayout(new BorderLayout());
        setResizable(false);
        setTitle("Export Ready");

        messagePane.setContentType("text/html");
        messagePane.setOpaque(false);
        messagePane.setEditable(false);
        messagePane.addHyperlinkListener(JHVGlobals.hyperOpenURL);
        messagePane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        messagePane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(messagePane, BorderLayout.CENTER);

        JPanel closeButtonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        closeButtonContainer.add(closeButton);
        add(closeButtonContainer, BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(closeButton);
        getRootPane().setFocusable(true);
    }

    public void init(String message) {
        messagePane.setText(message);
    }

    @Override
    public void init() {
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

}
