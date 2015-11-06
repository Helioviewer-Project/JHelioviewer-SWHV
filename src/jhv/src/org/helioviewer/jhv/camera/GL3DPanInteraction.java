package org.helioviewer.jhv.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Vec2d;
import org.helioviewer.jhv.display.Displayer;

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

        Vec2d pan = camera.getPanning();
        pan.x += x * m;
        pan.y -= y * m;
        camera.setPanning(pan);
        camera.updateCameraTransformation();

        lastMousePoint = p;
        Displayer.render();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Displayer.render();
    }

}
