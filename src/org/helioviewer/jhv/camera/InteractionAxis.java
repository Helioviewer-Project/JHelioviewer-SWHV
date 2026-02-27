package org.helioviewer.jhv.camera;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.Vec3;

class InteractionAxis implements Interaction.Type {

    private final Camera camera;
    private double trackballRadius2 = Sun.Radius2;
    private int lastMouseX;
    private int lastMouseY;
    private boolean dragStartSet; // avoid freak mouseDragged before mousePressed
    private Vec3 dragAxis = Vec3.YAxis; // cached drag axis

    InteractionAxis(Camera _camera) {
        camera = _camera;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Viewport vp = Display.getActiveViewport();
        trackballRadius2 = CameraHelper.selectTrackballRadius2(camera, vp, e.getX(), e.getY());
        dragAxis = camera.getUpdateViewpoint() == UpdateViewpoint.equatorial ? Vec3.ZAxis : Vec3.YAxis;
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        dragStartSet = true;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragStartSet)
            return;
        if ((e.getX() == lastMouseX) && (e.getY() == lastMouseY))
            return;

        Viewport vp = Display.getActiveViewport();
        camera.rotateDragRotation(CameraHelper.calcTrackballDelta(camera, vp, lastMouseX, lastMouseY, e.getX(), e.getY(), trackballRadius2).twist(dragAxis));
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        MovieDisplay.display();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragStartSet = false;
    }

}
