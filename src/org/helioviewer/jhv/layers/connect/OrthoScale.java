package org.helioviewer.jhv.layers.connect;

import org.helioviewer.jhv.math.Vec3;

public class OrthoScale {

    private static final double radius = 1.01;

    public final Vec3 ortho;
    public final Vec3 scale;
    public final byte[] color;

    OrthoScale(Vec3 v, byte[] _color) {
        ortho = new Vec3(radius * v.x, radius * v.y, radius * v.z);
        scale = new Vec3(v.x, -v.y, v.z);
        color = _color;
    }

}
