package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;

public abstract class Viewpoint {

    public JHVDate time;
    public Quat orientation;
    public double distance;

    abstract void update(JHVDate date);
    abstract CameraOptionPanel getOptionPanel();

    Viewpoint() {
        time = TimeUtils.epoch;
        orientation = Quat.ZERO;
        distance = Sun.MeanEarthDistance;
    }

    public final Viewpoint copy() {
        try {
            Viewpoint v = this.getClass().newInstance();
            v.time = time;
            v.orientation = orientation.copy();
            v.distance = distance;
            return v;
        } catch (Exception e) {
            return this;
        }
    }

    @Override
    public final String toString() {
        return "[time" + time + ", orientation=" + orientation + ", distance=" + distance + "]";
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof Viewpoint) {
            Viewpoint v = (Viewpoint) o;
            return time.equals(v.time) && orientation.equals(v.orientation) && distance == v.distance;
        }
        return false;
    }

    @Override
    public final int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

}
