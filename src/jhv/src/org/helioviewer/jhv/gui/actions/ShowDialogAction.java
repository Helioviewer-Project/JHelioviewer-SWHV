package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.dialogs.HelpDialog;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

/**
 * Action to show any given dialog.
 *
 * @author Markus Langenberg
 */

public class ShowDialogAction extends AbstractAction {

    private final Class<ShowableDialog> dialogToShow;
    private ShowableDialog dialog;

    /**
     * Default constructor.
     *
     * @param name
     *            name of the action that shall be displayed on a button
     * @param dialog
     *            Dialog to open on click
     */
    @SuppressWarnings("unchecked")
    public <T extends ShowableDialog> ShowDialogAction(String name, Class<T> dialog) {
        super(name);

        dialogToShow = (Class<ShowableDialog>) dialog;
        if (dialog.isAssignableFrom(HelpDialog.class)) {
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if (dialog == null)
                dialog = dialogToShow.newInstance();
            dialog.init();
            dialog.showDialog();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

}
