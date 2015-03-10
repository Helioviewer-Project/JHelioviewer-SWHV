package org.helioviewer.gl3d.gui;

import java.awt.event.ActionEvent;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.actions.ZoomOutAction;

/**
 * Action that zooms out, which increases the {@link GL3DCamera}'s distance to
 * the sun.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DZoomOutAction extends ZoomOutAction {

    private static final long serialVersionUID = 1L;

    public GL3DZoomOutAction(boolean small) {
        super(small);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GL3DCamera camera = GL3DState.get().getActiveCamera();
        camera.setCameraFOV(camera.getCameraFOV() / 0.95);
        camera.updateCameraTransformation(true);
        Displayer.display();
    }

}
