package org.helioviewer.jhv.event;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.Vec3;

public class EventGeometry {

    private final Vec3 centralPoint;
    private final float[] boundBox;
    private final Position earthPosition;

    public EventGeometry(Vec3 _centralPoint, float[] _boundBox, Position p) {
        centralPoint = _centralPoint;
        boundBox = _boundBox;
        earthPosition = p;
    }

    public Vec3 centralPoint() {
        return centralPoint;
    }

    public float[] getBoundBox() {
        return boundBox;
    }

    public Position getEarth() {
        return earthPosition;
    }

}
