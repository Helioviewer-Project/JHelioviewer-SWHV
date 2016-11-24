package org.helioviewer.jhv.gui.dialogs.observation;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public abstract class ObservationDialogPanel extends JPanel {

    protected static final int GRIDLAYOUT_HGAP = 5;
    protected static final int GRIDLAYOUT_VGAP = 2;

    public abstract boolean loadButtonPressed(Object layer);

}
