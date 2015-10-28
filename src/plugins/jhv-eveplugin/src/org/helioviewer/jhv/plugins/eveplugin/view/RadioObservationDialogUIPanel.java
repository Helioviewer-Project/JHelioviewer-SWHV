package org.helioviewer.jhv.plugins.eveplugin.view;

import org.helioviewer.jhv.gui.dialogs.observation.ServerListPanel;

public class RadioObservationDialogUIPanel extends SimpleObservationDialogUIPanel {

    public RadioObservationDialogUIPanel() {
        super();
        add(new ServerListPanel("Server"));
    }

}
