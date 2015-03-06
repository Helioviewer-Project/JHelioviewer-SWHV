package org.helioviewer.gl3d.camera;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;

/**
 * Default {@link GL3DInteraction} class that provides a reference to the
 * {@link GL3DSceneGraphView}. Default behavior includes camera reset on double
 * click.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public abstract class GL3DDefaultInteraction extends GL3DInteraction {

    private static final double ZOOM_WHEEL_FACTOR = 0.002;

    protected GL3DSceneGraphView sceneGraphView;

    protected GL3DDefaultInteraction(GL3DCamera camera, GL3DSceneGraphView sceneGraph) {
        super(camera);
        this.sceneGraphView = sceneGraph;
    }

    @Override
    public void reset(GL3DCamera camera) {
    }

    @Override
    public void mouseClicked(MouseEvent e, GL3DCamera camera) {
        if (e.getClickCount() == 2) {
            camera.reset();
        }
    }

    public void reset() {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e, GL3DCamera camera) {
        int wr = e.getWheelRotation();
        double previousFOV = camera.getCameraFOV();

        camera.setCameraFOV(camera.getCameraFOV() + ZOOM_WHEEL_FACTOR * wr);
        if (previousFOV != camera.getCameraFOV()) {
            Displayer.getSingletonInstance().display();
        }
    }

}
