package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;

class InteractionAxis extends InteractionTrackball {

    private Vec3 dragAxis = Vec3.YAxis; // cached drag axis

    InteractionAxis(Camera _camera) {
        super(_camera);
    }

    @Override
    public void mousePressed(PointerEvent e, Viewport vp) {
        super.mousePressed(e, vp);
        dragAxis = Display.getViewpointUpdate().dragAxis();
    }

    @Override
    protected Quat adaptDelta(Quat delta) {
        return delta.twist(dragAxis);
    }

}
