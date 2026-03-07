package org.helioviewer.jhv.camera;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;

class InteractionAxis extends InteractionTrackball {

    private Vec3 dragAxis = Vec3.YAxis; // cached drag axis

    InteractionAxis(Camera _camera) {
        super(_camera);
    }

    @Override
    public void mousePressed(MouseEvent e, Viewport vp) {
        super.mousePressed(e, vp);
        dragAxis = camera.getUpdateViewpoint().dragAxis();
    }

    @Override
    protected Quat adaptDelta(Quat delta) {
        return delta.twist(dragAxis);
    }

}
