package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.JHVUpdate;

@SuppressWarnings("serial")
public class CheckUpdateAction extends AbstractAction {

    public CheckUpdateAction() {
        super("Check for Updates...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            JHVUpdate update = new JHVUpdate(true);
            update.check();
        } catch (Exception ex) {
            // Should not happen
            Log.error("Error while parsing update URL " + ex.getLocalizedMessage(), ex);
        }
    }

}
