package org.helioviewer.jhv.camera;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.MovieDisplay;

class InteractionRotate implements Interaction.Type {

    private final Camera camera;
    private double trackballRadius2 = Sun.Radius2;
    private int lastMouseX;
    private int lastMouseY;
    private boolean dragStartSet; // avoid freak mouseDragged before mousePressed

    InteractionRotate(Camera _camera) {
        camera = _camera;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Viewport vp = Display.getActiveViewport();
        trackballRadius2 = CameraHelper.selectTrackballRadius2(camera, vp, e.getX(), e.getY());
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
        camera.rotateDragRotation(CameraHelper.calcTrackballDelta(camera, vp, lastMouseX, lastMouseY, e.getX(), e.getY(), trackballRadius2));
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        MovieDisplay.display();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragStartSet = false;
    }

}
