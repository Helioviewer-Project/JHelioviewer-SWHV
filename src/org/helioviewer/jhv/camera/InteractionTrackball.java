package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.input.PointerEvent;
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
    public void mousePressed(PointerEvent e, Viewport vp) {
        trackballRadius2 = CameraHelper.selectTrackballRadius2(camera, vp, e.x(), e.y());
        lastMouseX = e.x();
        lastMouseY = e.y();
        dragStartSet = true;
    }

    @Override
    public void mouseDragged(PointerEvent e, Viewport vp) {
        if (!dragStartSet)
            return;
        if ((e.x() == lastMouseX) && (e.y() == lastMouseY))
            return;

        Quat delta = CameraHelper.calcTrackballDelta(camera, vp, lastMouseX, lastMouseY, e.x(), e.y(), trackballRadius2);
        camera.rotateDragRotation(adaptDelta(delta), Display.getViewpoint());
        lastMouseX = e.x();
        lastMouseY = e.y();
        MovieDisplay.display();
    }

    @Override
    public void mouseReleased() {
        dragStartSet = false;
    }

    protected abstract Quat adaptDelta(Quat delta);

}
