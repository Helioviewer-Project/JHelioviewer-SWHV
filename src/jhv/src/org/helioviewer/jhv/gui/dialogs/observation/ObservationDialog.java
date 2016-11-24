package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layer;

@SuppressWarnings("serial")
public class ObservationDialog extends JDialog implements ActionListener {

    private final JButton btnImages = new JButton();
    private final JButton btnClose = new JButton("Cancel");
    private final JButton availabilityButton = new JButton("Available data");

    private final ImageDataPanel observationPanel = new ImageDataPanel();
    private Object layer;

    private static ObservationDialog instance;

    public static ObservationDialog getInstance() {
        if (instance == null) {
            instance = new ObservationDialog(ImageViewerGui.getMainFrame());
        }
        return instance;
    }

    public ImageDataPanel getObservationPanel() {
        return observationPanel;
    }

    private ObservationDialog(JFrame mainFrame) {
        super(mainFrame, true);

        JPanel contentPane = new JPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        contentPane.setBorder(BorderFactory.createEmptyBorder(3, 9, 1, 9));
        contentPane.setFocusable(true);

        availabilityButton.addActionListener(e -> {
            String url = Settings.getSingletonInstance().getProperty("availability.images.url");
            int sourceId = observationPanel.getSourceId();
            if (sourceId != -1)
                url += "#IID" + sourceId;

            JHVGlobals.openURL(url);
        });

        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 3));
        buttonPane.add(availabilityButton);
        buttonPane.add(btnClose);
        buttonPane.add(btnImages);

        btnImages.addActionListener(this);
        btnClose.addActionListener(this);

        contentPane.add(observationPanel);
        contentPane.add(buttonPane);

        pack();
        Dimension dim = getPreferredSize();
        if (dim != null) { // satisfy coverity
            setMinimumSize(dim);
            pack();
        }

        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // Shows up the dialog and initializes the UI with the given panel.
    public void showDialog(boolean newLayer, Object _layer) {
        layer = _layer;
        observationPanel.setupLayer(layer);

        if (newLayer) {
            setTitle("New Layer");
            btnImages.setText("Add");
        } else {
            setTitle("Change Layer");
            btnImages.setText("Change");
        }

        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        getRootPane().setDefaultButton(btnImages);
        setVisible(true);
    }

    public void loadButtonPressed() {
        if (observationPanel.loadButtonPressed(layer))
            dispose();
    }

    public void setAvailabilityStatus(boolean status) {
        availabilityButton.setEnabled(status);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnImages)) {
            loadButtonPressed();
        } else if (e.getSource().equals(btnClose)) {
            if (layer instanceof Layer)
                ((Layer) layer).unload();
            dispose();
        }
    }

}
