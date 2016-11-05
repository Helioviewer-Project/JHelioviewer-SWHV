package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

// Action to show any given dialog
@SuppressWarnings("serial")
public class ShowDialogAction extends AbstractAction {

    private final Class<ShowableDialog> dialogToShow;
    private ShowableDialog dialog;

    /**
     * @param name
     *            name of the action that shall be displayed on a button
     * @param dialog
     *            Dialog to open on click
     */
    @SuppressWarnings("unchecked")
    public <T extends ShowableDialog> ShowDialogAction(String name, Class<T> dialog) {
        super(name);
        dialogToShow = (Class<ShowableDialog>) dialog;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if (dialog == null)
                dialog = dialogToShow.getConstructor().newInstance();
            dialog.init();
            dialog.showDialog();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
