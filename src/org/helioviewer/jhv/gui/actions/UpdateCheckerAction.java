package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.io.UpdateChecker;

@SuppressWarnings("serial")
public class UpdateCheckerAction extends AbstractAction {

    public UpdateCheckerAction() {
        super("Check for Updates...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        UpdateChecker.check(true);
    }

}
