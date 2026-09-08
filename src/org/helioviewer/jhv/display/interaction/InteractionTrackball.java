package org.helioviewer.jhv.display.interaction;

import javax.annotation.Nullable;

import org.helioviewer.jhv.display.Camera;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.display.ViewportMath;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;

final class InteractionTrackball extends Interaction.Type {

    private enum Constraint {NONE, AXIS}

    private final Camera camera;
    private final Constraint constraint;
    @Nullable
    private Vec3 axisOverride;
    private double trackballRadius2;
    private Vec3 defaultAxis;
    private int lastMouseX;
    private int lastMouseY;

    private InteractionTrackball(Camera _camera, Constraint _constraint) {
        camera = _camera;
        constraint = _constraint;
    }

    static InteractionTrackball rotate(Camera camera) {
        return new InteractionTrackball(camera, Constraint.NONE);
    }

    static InteractionTrackball axis(Camera camera) {
        return new InteractionTrackball(camera, Constraint.AXIS);
    }

    void setAxisOverride(@Nullable Vec3 axis) {
        axisOverride = axis;
    }

    @Override
    void mousePressed(PointerEvent e, Viewport vp) {
        trackballRadius2 = ViewportMath.selectTrackballRadius2(camera, vp, e.x(), e.y());
        if (constraint == Constraint.AXIS)
            defaultAxis = DisplayController.getViewpointUpdate().dragAxis();
        lastMouseX = e.x();
        lastMouseY = e.y();
    }

    @Override
    void mouseDragged(PointerEvent e, Viewport vp) {
        if (e.x() == lastMouseX && e.y() == lastMouseY)
            return;

        Quat delta = ViewportMath.calcTrackballDelta(camera, vp, lastMouseX, lastMouseY, e.x(), e.y(), trackballRadius2);
        if (constraint == Constraint.AXIS)
            delta = delta.twist(axisOverride == null ? defaultAxis : axisOverride);
        camera.rotateDragRotation(delta);
        lastMouseX = e.x();
        lastMouseY = e.y();
        DisplayController.display();
    }

}
