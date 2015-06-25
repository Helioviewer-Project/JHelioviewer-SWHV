package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;

/**
 * Sets the interaction of the current camera to Zoom Box Interaction
 */
@SuppressWarnings("serial")
public class SetZoomBoxInteractionAction extends AbstractAction {

    public SetZoomBoxInteractionAction() {
        super("Zoom Box");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GL3DCamera cam = Displayer.getActiveCamera();
        cam.setCurrentInteraction(cam.getZoomInteraction());
    }

}
