package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.camera.GL3DSolarRotationTrackingTrackballCamera;
import org.helioviewer.jhv.display.Displayer;

/**
 * Action that enables the Solar Rotation Tracking, which ultimately changes the
 * current {@link GL3DCamera} to the
 * {@link GL3DSolarRotationTrackingTrackballCamera}
 */
public class ToggleSolarRotationAction extends AbstractAction {

    public ToggleSolarRotationAction() {
        super("Track");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        GL3DCamera cam = Displayer.getActiveCamera();
        cam.setTrackingMode(!cam.getTrackingMode());
    }

}
