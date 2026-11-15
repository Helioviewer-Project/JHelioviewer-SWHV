package org.helioviewer.jhv.event;

import org.helioviewer.jhv.math.Vec3;

public class EventGeometry {

    private final Vec3 centralPoint;
    private final float[] boundBox;

    public EventGeometry(Vec3 _centralPoint, float[] _boundBox) {
        centralPoint = _centralPoint;
        boundBox = _boundBox;
    }

    public Vec3 centralPoint() {
        return centralPoint;
    }

    public float[] getBoundBox() {
        return boundBox;
    }

}
