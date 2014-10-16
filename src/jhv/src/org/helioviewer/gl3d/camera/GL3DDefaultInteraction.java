package org.helioviewer.gl3d.camera;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.MetaDataView;

/**
 * Default {@link GL3DInteraction} class that provides a reference to the
 * {@link GL3DSceneGraphView}. Default behavior includes camera reset on double
 * click.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public abstract class GL3DDefaultInteraction extends GL3DInteraction {
    private static final double ZOOM_WHEEL_FACTOR = 1.0 / 20;

    protected GL3DSceneGraphView sceneGraphView;

    protected GL3DDefaultInteraction(GL3DCamera camera, GL3DSceneGraphView sceneGraph) {
        super(camera);
        this.sceneGraphView = sceneGraph;
        this.reset();
    }

    @Override
    public void reset(GL3DCamera camera) {
        reset();
    }

    @Override
    public void mouseClicked(MouseEvent e, GL3DCamera camera) {
        if (e.getClickCount() == 2) {
            reset();
        }
    }

    public void reset() {
        MetaDataView view = sceneGraphView.getAdapter(MetaDataView.class);
        if (view != null && view.getMetaData() != null) {
            Region region = view.getMetaData().getPhysicalRegion();
            double halfWidth = region.getWidth() / 2;
            double halfFOVRad = Math.toRadians(camera.getCameraFOV() / 2);
            double distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad) / Math.sin(halfFOVRad);
            distance = -distance - camera.getZTranslation();
            // Log.debug("GL3DZoomFitAction: Distance = "+distance+" Existing Distance: "+camera.getZTranslation());
            camera.setZTranslation(distance);
            GL3DVec3d cameraTranslation = this.camera.getTranslation().copy();
            cameraTranslation.negate();
            camera.addCameraAnimation(new GL3DCameraPanAnimation(cameraTranslation));
        } else {
            camera.setZTranslation(-GL3DEarthCamera.DEFAULT_CAMERA_DISTANCE);
            this.camera.updateCameraTransformation();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e, GL3DCamera camera) {
        double fovDistance = -camera.getCameraFOV() / 2 * GL3DDefaultInteraction.ZOOM_WHEEL_FACTOR;
        System.out.println("FOVDIST" + fovDistance);
        camera.addCameraAnimation(new GL3DCameraZoomAnimation(fovDistance));
    }

}
