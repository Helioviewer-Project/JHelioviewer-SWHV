package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Quat;

public final class DisplayView {

    private final Position viewpoint;
    private final double cameraWidth;
    private final Quat viewRotation;

    DisplayView(Position _viewpoint, double _cameraWidth, Quat _viewRotation) {
        viewpoint = _viewpoint;
        cameraWidth = _cameraWidth;
        viewRotation = _viewRotation;
    }

    public Position viewpoint() {
        return viewpoint;
    }

    public double cameraWidth(Viewport vp) {
        return cameraWidth * vp.zoom;
    }

    public Quat viewRotation() {
        return viewRotation;
    }
}
