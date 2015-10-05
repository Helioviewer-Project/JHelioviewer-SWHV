package org.helioviewer.jhv.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.base.math.GL3DVec3d;
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

    protected GL3DPanInteraction(GL3DCamera camera) {
        super(camera);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        lastMousePoint = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        int x = p.x - lastMousePoint.x;
        int y = p.y - lastMousePoint.y;
        double m = 2. * camera.getCameraWidth() / Displayer.getViewport().getHeight();

        GL3DVec3d tran = camera.getTranslation();
        tran.x += x * m;
        tran.y -= y * m;
        camera.setPanning(tran.x, tran.y);
        camera.updateCameraTransformation();

        lastMousePoint = p;
        Displayer.render();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Displayer.render();
    }

}
