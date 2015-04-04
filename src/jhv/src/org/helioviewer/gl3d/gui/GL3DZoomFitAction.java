package org.helioviewer.gl3d.gui;

import java.awt.event.ActionEvent;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.actions.ZoomFitAction;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * Action that zooms in or out to fit the currently displayed image layers to
 * the displayed viewport. For 3D this results in a change in the
 * {@link GL3DCamera}'s distance to the sun.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DZoomFitAction extends ZoomFitAction {

    private static final long serialVersionUID = 1L;

    public GL3DZoomFitAction(boolean small) {
        super(small);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JHVJP2View view = Displayer.getLayersModel().getActiveView();
        GL3DCamera camera = GL3DCameraSelectorModel.getInstance().getCurrentCamera();
        if (view != null) {
            Region region = view.getMetaData().getPhysicalRegion();
            if (region != null) {
                double halfWidth = region.getWidth() / 2;
                double halfFOVRad = Math.toRadians(camera.getCameraFOV() / 2.0);
                double distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad) / Math.sin(halfFOVRad);
                distance = -distance - camera.getZTranslation();
                GL3DVec3d cameraTranslation = camera.getTranslation().copy();
                cameraTranslation.negate();
            }
        }
    }

}
