package org.helioviewer.jhv.display.interaction;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.DisplayFrame;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.display.ViewportProjection;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;

final class InteractionTrackball extends Interaction.Type {

    private enum Constraint {NONE, AXIS}

    final Camera camera;
    private final Constraint constraint;
    private double trackballRadius2 = Sun.Radius2;
    private Vec3 dragAxis = Vec3.YAxis; // cached drag axis
    private int lastMouseX;
    private int lastMouseY;
    private boolean dragStartSet; // avoid freak mouseDragged before mousePressed

    InteractionTrackball(Camera _camera, Constraint _constraint) {
        camera = _camera;
        constraint = _constraint;
    }

    static InteractionTrackball rotate(Camera camera) {
        return new InteractionTrackball(camera, Constraint.NONE);
    }

    static InteractionTrackball axis(Camera camera) {
        return new InteractionTrackball(camera, Constraint.AXIS);
    }

    @Override
    void mousePressed(PointerEvent e, Viewport vp) {
        trackballRadius2 = ViewportProjection.selectTrackballRadius2(camera, vp, e.x(), e.y());
        if (constraint == Constraint.AXIS)
            dragAxis = DisplayFrame.getViewpointUpdate().dragAxis();
        lastMouseX = e.x();
        lastMouseY = e.y();
        dragStartSet = true;
    }

    @Override
    void mouseDragged(PointerEvent e, Viewport vp) {
        if (!dragStartSet)
            return;
        if ((e.x() == lastMouseX) && (e.y() == lastMouseY))
            return;

        Quat delta = ViewportProjection.calcTrackballDelta(camera, vp, lastMouseX, lastMouseY, e.x(), e.y(), trackballRadius2);
        camera.rotateDragRotation(constraint == Constraint.AXIS ? delta.twist(dragAxis) : delta);
        lastMouseX = e.x();
        lastMouseY = e.y();
        DisplayFrame.display();
    }

    @Override
    void mouseReleased() {
        dragStartSet = false;
    }

}
