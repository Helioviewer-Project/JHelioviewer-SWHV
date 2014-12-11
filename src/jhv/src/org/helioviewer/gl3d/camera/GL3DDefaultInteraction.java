package org.helioviewer.gl3d.camera;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.display.Displayer;

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
        camera.setZTranslation(-GL3DEarthCamera.DEFAULT_CAMERA_DISTANCE);
        this.camera.updateCameraTransformation();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e, GL3DCamera camera) {
        int wr = e.getWheelRotation();

        camera.setCameraFOV(camera.getCameraFOV() + 0.02 * ZOOM_WHEEL_FACTOR * wr);
        camera.updateCameraTransformation(true);
        Displayer.getSingletonInstance().display();
    }
}
