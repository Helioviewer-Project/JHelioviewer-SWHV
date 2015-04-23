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
        Point p = e.getPoint();
        int x = p.x - this.lastMousePoint.x;
        int y = p.y - this.lastMousePoint.y;
        double m = 2. * camera.getCameraWidth() / GL3DState.getViewportHeight();

        camera.translation.x += x * m;
        camera.translation.y -= y * m;

        this.lastMousePoint = p;
        camera.updateCameraTransformation();

        Displayer.display();
    }

    @Override
    public void mouseReleased(MouseEvent e, GL3DCamera camera) {
        Displayer.display();
    }

}
