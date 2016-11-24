package org.helioviewer.jhv.plugins.eveplugin.view;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.plugins.eveplugin.EVESettings;

@SuppressWarnings("serial")
public class TimelineDialog extends JDialog {

    private final TimelineDataPanel observationPanel = new TimelineDataPanel();
    private final JButton btnImages = new JButton("Add");

    public TimelineDialog() {
        super(ImageViewerGui.getMainFrame(), true);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        contentPane.setBorder(BorderFactory.createEmptyBorder(3, 9, 1, 9));
        contentPane.setFocusable(true);
        setContentPane(contentPane);

        JButton availabilityButton = new JButton("Available data");
        availabilityButton.addActionListener(e -> JHVGlobals.openURL(EVESettings.availabilityURL));

        btnImages.addActionListener(e -> loadButtonPressed());
        JButton btnClose = new JButton("Cancel");
        btnClose.addActionListener(e -> dispose());

        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 3));
        buttonPane.add(availabilityButton);
        buttonPane.add(btnClose);
        buttonPane.add(btnImages);

        contentPane.add(observationPanel);
        contentPane.add(buttonPane);

        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public TimelineDataPanel getObservationPanel() {
        return observationPanel;
    }

    public void showDialog() {
        pack();
        Dimension dim = getPreferredSize();
        if (dim != null) { // satisfy coverity
            setMinimumSize(dim);
            pack();
        }

        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        getRootPane().setDefaultButton(btnImages);
        setVisible(true);
    }

    private void loadButtonPressed() {
        if (observationPanel.loadButtonPressed(null))
            dispose();
    }

}
