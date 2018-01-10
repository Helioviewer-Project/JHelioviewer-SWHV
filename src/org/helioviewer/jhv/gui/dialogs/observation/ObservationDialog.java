package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.ImageLayer;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class ObservationDialog extends StandardDialog {

    private final AbstractAction load = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            loadButtonPressed();
        }
    };
    private final JButton okBtn = new JButton(load);
    private final JButton availabilityBtn = new JButton("Available data");

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
        setResizable(false);

        availabilityBtn.addActionListener(e -> JHVGlobals.openURL(observationPanel.getAvailabilityURL()));
        setInitFocusedComponent(observationPanel.getFocused());
        setDefaultAction(load);
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                layer.unload();
                layer = null;
                setVisible(false);
            }
        };
        setDefaultCancelAction(close);

        JButton cancelBtn = new JButton(close);
        cancelBtn.setText("Cancel");

        ButtonPanel panel = new ButtonPanel();
        panel.add(okBtn, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(cancelBtn, ButtonPanel.CANCEL_BUTTON);
        panel.add(availabilityBtn, ButtonPanel.OTHER_BUTTON);

        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        observationPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        return observationPanel;
    }

    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    // Shows up the dialog and initializes the UI with the given panel.
    public void showDialog(boolean newLayer, ImageLayer _layer) {
        layer = _layer;
        observationPanel.setupLayer(layer);

        if (newLayer) {
            setTitle("New Layer");
            okBtn.setText("Add");
        } else {
            setTitle("Change Layer");
            okBtn.setText("Change");
        }

        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    public void loadButtonPressed() {
        if (observationPanel.doLoad(layer))
            setVisible(false);
    }

    public void setAvailabilityStatus(boolean status) {
        availabilityBtn.setEnabled(status);
    }

}
