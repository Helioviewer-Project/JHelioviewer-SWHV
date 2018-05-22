package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.JHVUpdate;

@SuppressWarnings("serial")
public class CheckUpdateAction extends AbstractAction {

    public CheckUpdateAction() {
        super("Check for Updates...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new JHVUpdate(true).check();
    }

}
