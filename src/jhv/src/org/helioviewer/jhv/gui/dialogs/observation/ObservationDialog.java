package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
import org.helioviewer.jhv.layers.ImageLayer;

@SuppressWarnings("serial")
public class ObservationDialog extends JDialog {

    private final JButton btnImages = new JButton();
    private final JButton availabilityButton = new JButton("Available data");

    private final ImageDataPanel observationPanel = new ImageDataPanel();
    private ImageLayer layer;

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

        btnImages.addActionListener(e -> loadButtonPressed());
        JButton btnClose = new JButton("Cancel");
        btnClose.addActionListener(e -> cancel());

        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 3));
        buttonPane.add(availabilityButton);
        buttonPane.add(btnClose);
        buttonPane.add(btnImages);

        contentPane.add(observationPanel);
        contentPane.add(buttonPane);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancel();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                observationPanel.focusTree();
            }
        });

        getRootPane().registerKeyboardAction(e -> cancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // Shows up the dialog and initializes the UI with the given panel.
    public void showDialog(boolean newLayer, ImageLayer _layer) {
        layer = _layer;
        observationPanel.setupLayer(layer);

        if (newLayer) {
            setTitle("New Layer");
            btnImages.setText("Add");
        } else {
            setTitle("Change Layer");
            btnImages.setText("Change");
        }

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

    public void loadButtonPressed() {
        if (observationPanel.loadButtonPressed(layer))
            dispose();
    }

    public void setAvailabilityStatus(boolean status) {
        availabilityButton.setEnabled(status);
    }

    private void cancel() {
        layer.unload();
        dispose();
    }

}
