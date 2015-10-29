package org.helioviewer.jhv.plugins.eveplugin.view;

import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.dialogs.observation.ServerListCombo;

@SuppressWarnings("serial")
public class RadioObservationDialogUIPanel extends SimpleObservationDialogUIPanel {

    public RadioObservationDialogUIPanel() {
        super();
        JLabel labelServer = new JLabel("Server", JLabel.RIGHT);
        JPanel container = new JPanel();

        container.setLayout(new GridLayout(1, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
        container.add(labelServer);
        container.add(ServerListCombo.getInstance());
        add(container);
    }

}
