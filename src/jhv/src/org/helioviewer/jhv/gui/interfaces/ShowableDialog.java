package org.helioviewer.jhv.gui.interfaces;

/**
 * Interface representing a dialog.
 * 
 * <p>
 * This interface was introduce to generalize all dialogs used in JHV, keeping
 * the ability to show them. For example, this is used for
 * {@link org.helioviewer.jhv.gui.actions.ShowDialogAction}.
 * 
 * @author Markus Langenberg
 * 
 */
public interface ShowableDialog {

    /**
     * Shows the dialog on the screen.
     */
    public void showDialog();
}
