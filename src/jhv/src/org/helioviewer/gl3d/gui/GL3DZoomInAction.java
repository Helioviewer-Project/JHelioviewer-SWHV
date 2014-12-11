package org.helioviewer.gl3d.gui;

import java.awt.event.ActionEvent;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.actions.ZoomInAction;

/**
 * Action that zooms in, which reduces the distance of the {@link GL3DCamera} to
 * the sun.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DZoomInAction extends ZoomInAction {

    private static final long serialVersionUID = 1L;

    public GL3DZoomInAction(boolean small) {
        super(small);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GL3DCamera camera = GL3DState.get().getActiveCamera();
        camera.setCameraFOV(camera.getCameraFOV() + 0.01);
        camera.updateCameraTransformation(true);
        Displayer.getSingletonInstance().display();
    }

}
