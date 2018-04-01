package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.display.Display;

@SuppressWarnings("serial")
public class ResetCameraAction extends AbstractAction {

    public ResetCameraAction() {
        super("Reset Camera");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Display.getCamera().reset();
    }

}
