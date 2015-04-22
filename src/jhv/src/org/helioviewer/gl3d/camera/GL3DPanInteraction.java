package org.helioviewer.gl3d.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.gl3d.GL3DState;
import org.helioviewer.jhv.display.Displayer;

/**
 * Standard panning interaction, moves the camera proportionally to the mouse
 * movement when dragging
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DPanInteraction extends GL3DDefaultInteraction {

    private Point lastMousePoint;

    protected GL3DPanInteraction(GL3DSolarRotationTrackingTrackballCamera camera) {
        super(camera);
    }

    @Override
    public void mousePressed(MouseEvent e, GL3DCamera camera) {
        this.lastMousePoint = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e, GL3DCamera camera) {
        int x = e.getPoint().x - this.lastMousePoint.x;
        int y = e.getPoint().y - this.lastMousePoint.y;
        GL3DState state = GL3DState.get();
        camera.translation.x += x * 2. * camera.getCameraWidth() / state.getViewportHeight();
        camera.translation.y -= y * 2. * camera.getCameraWidth() / state.getViewportHeight();

        this.lastMousePoint = e.getPoint();
        camera.updateCameraTransformation();

        Displayer.display();
    }

    @Override
    public void mouseReleased(MouseEvent e, GL3DCamera camera) {
        Displayer.display();
    }

}
