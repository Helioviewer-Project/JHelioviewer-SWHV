package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.UpdateChecker;

@SuppressWarnings("serial")
public class UpdateCheckerAction extends AbstractAction {

    public UpdateCheckerAction() {
        super("Check for Updates...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new UpdateChecker(true).check();
    }

}
