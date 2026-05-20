package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.Quat;

public record MapContext(Position viewpoint, Viewport vp, ProjectionScale scale, GridType gridType, Quat rotation) {

    public MapContext(Position viewpoint, Viewport vp, ProjectionScale scale, GridType gridType) {
        this(viewpoint, vp, scale, gridType, scale.isOrtho() ? null : gridType.mapRotation(viewpoint));
    }

}
