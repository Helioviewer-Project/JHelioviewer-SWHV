package org.helioviewer.jhv.camera;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.Quat;

abstract class InteractionTrackball implements Interaction.Type {

    protected final Camera camera;
    private double trackballRadius2 = Sun.Radius2;
    private int lastMouseX;
    private int lastMouseY;
    private boolean dragStartSet; // avoid freak mouseDragged before mousePressed

    InteractionTrackball(Camera _camera) {
        camera = _camera;
    }

    @Override
    public void mousePressed(MouseEvent e, Viewport vp, Interaction.AnnotationMode annotationMode) {
        trackballRadius2 = CameraHelper.selectTrackballRadius2(camera, vp, e.getX(), e.getY());
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        dragStartSet = true;
    }

    @Override
    public void mouseDragged(MouseEvent e, Viewport vp) {
        if (!dragStartSet)
            return;
        if ((e.getX() == lastMouseX) && (e.getY() == lastMouseY))
            return;

        Quat delta = CameraHelper.calcTrackballDelta(camera, vp, lastMouseX, lastMouseY, e.getX(), e.getY(), trackballRadius2);
        camera.rotateDragRotation(adaptDelta(delta));
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        MovieDisplay.display();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragStartSet = false;
    }

    protected abstract Quat adaptDelta(Quat delta);

}
