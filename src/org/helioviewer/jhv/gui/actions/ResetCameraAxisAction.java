package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.layers.MovieDisplay;

@SuppressWarnings("serial")
public class ResetCameraAxisAction extends AbstractAction {

    public ResetCameraAxisAction() {
        super("Reset Camera Axis");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Display.getCamera().resetDragRotationAxis();
        MovieDisplay.display();
    }

}
