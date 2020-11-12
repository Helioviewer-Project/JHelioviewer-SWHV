package org.helioviewer.jhv.astronomy;

import org.helioviewer.jhv.time.JHVTime;

public class PositionCartesian {

    public final double x;
    public final double y;
    public final double z;
    public final JHVTime time;

    public PositionCartesian(JHVTime _time, double _x, double _y, double _z) {
        x = _x;
        y = _y;
        z = _z;
        time = _time;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PositionCartesian))
            return false;
        PositionCartesian p = (PositionCartesian) o;
        return Double.doubleToLongBits(x) == Double.doubleToLongBits(p.x) &&
                Double.doubleToLongBits(y) == Double.doubleToLongBits(p.y) &&
                Double.doubleToLongBits(z) == Double.doubleToLongBits(p.z) &&
                time.equals(p.time);
    }

    @Override
    public int hashCode() {
        int result = 1;
        long tmp = Double.doubleToLongBits(x);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        tmp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        tmp = Double.doubleToLongBits(z);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        return 31 * result + time.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s [%f,%f,%f]", time, x, y, z);
    }

}
