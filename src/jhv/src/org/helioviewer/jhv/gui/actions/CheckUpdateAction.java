package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.JHVUpdate;

/**
 * Action that checks for updates
 */
@SuppressWarnings("serial")
public class CheckUpdateAction extends AbstractAction {

    public CheckUpdateAction() {
        super("Check for Updates...");
        putValue(SHORT_DESCRIPTION, "Check for newer releases");
    }

    public void actionPerformed(ActionEvent arg0) {
        try {
            JHVUpdate update = new JHVUpdate();
            update.setVerbose(true);
            update.check();
        } catch (Exception e) {
            // Should not happen
            Log.error("Error while parsing update url " + e.getLocalizedMessage(), e);
        }
    }

}
