package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.display.Displayer;

@SuppressWarnings("serial")
public class ResetCameraAction extends AbstractAction {

    public ResetCameraAction() {
        super("Reset Camera");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Displayer.getCamera().reset();
    }

}
