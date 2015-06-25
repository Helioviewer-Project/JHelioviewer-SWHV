package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.layers.Layers;

/**
 * Action that enables the Solar Rotation Tracking, which changes the
 * current camera to tracking mode
 */
@SuppressWarnings("serial")
public class ToggleSolarRotationAction extends AbstractAction {

    public ToggleSolarRotationAction() {
        super("Track");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GL3DCamera cam = Layers.getActiveCamera();
        cam.setTrackingMode(!cam.getTrackingMode());
    }

}
