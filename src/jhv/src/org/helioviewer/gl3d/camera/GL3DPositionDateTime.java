package org.helioviewer.gl3d.camera;

import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;

public class GL3DPositionDateTime {
    private final GL3DVec3d position;
    private final long timestamp;

    public GL3DPositionDateTime(long timestamp, GL3DVec3d position) {
        this.timestamp = timestamp;
        this.position = position;
    }

    public GL3DVec3d getPosition() {
        return position;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return timestamp + " " + this.position;
    }
}
