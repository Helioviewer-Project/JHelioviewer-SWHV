package org.helioviewer.jhv.gui.dialogs.observation;

import javax.swing.JPanel;

/**
 * Abstract base class for UI components. Objects which are derived from this
 * base class can be added to the {@link ObservationDialog}.
 * 
 * @author Stephan Pagel
 * */
@SuppressWarnings("serial")
public abstract class ObservationDialogPanel extends JPanel {

    protected static final int GRIDLAYOUT_HGAP = 5;
    protected static final int GRIDLAYOUT_VGAP = 2;

    /**
     * This method will be called when the add button within the dialog has been
     * pressed.
     * */
    public abstract boolean loadButtonPressed();

}
