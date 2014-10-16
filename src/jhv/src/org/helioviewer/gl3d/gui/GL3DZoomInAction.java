package org.helioviewer.gl3d.gui;

import java.awt.event.ActionEvent;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraZoomAnimation;
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
        GL3DCamera camera = GL3DCameraSelectorModel.getInstance().getCurrentCamera();

        double distance = camera.getDistanceToSunSurface() / 3;
        GL3DCameraSelectorModel.getInstance().getCurrentCamera().addCameraAnimation(new GL3DCameraZoomAnimation(-0.1, 500));
        Displayer.getSingletonInstance().render();
    }

}
