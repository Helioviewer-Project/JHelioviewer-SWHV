package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.math.Quat;

class InteractionRotate extends InteractionTrackball {

    InteractionRotate(Camera _camera) {
        super(_camera);
    }

    @Override
    protected Quat adaptDelta(Quat delta) {
        return delta;
    }
}
