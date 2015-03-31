package org.helioviewer.gl3d.camera;

public class GL3DPositionDateTime {

    public final double x;
    public final double y;
    public final double z;
    public final long timestamp;

    public GL3DPositionDateTime(long timestamp, double x, double y, double z) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return String.format("%d [%f,%f,%f]", timestamp, x, y, z);
    }

}
