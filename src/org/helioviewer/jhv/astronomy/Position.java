package org.helioviewer.jhv.astronomy;

import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.time.JHVDate;

public class Position {

    public final double distance;
    public final double lon;
    public final double lat;
    public final JHVDate time;
    private Quat q;

    public Position(JHVDate _time, double _distance, double _lon, double _lat) {
        distance = _distance;
        lon = _lon;
        lat = _lat;
        time = _time;
    }

    public Quat toQuat() {
        if (q == null)
            q = new Quat(lat, lon);
        return q;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Position))
            return false;
        Position p = (Position) o;
        return Double.doubleToLongBits(distance) == Double.doubleToLongBits(p.distance) &&
                Double.doubleToLongBits(lon) == Double.doubleToLongBits(p.lon) &&
                Double.doubleToLongBits(lat) == Double.doubleToLongBits(p.lat) &&
                time.equals(p.time);
    }

    @Override
    public int hashCode() {
        int result = 1;
        long tmp = Double.doubleToLongBits(distance);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        tmp = Double.doubleToLongBits(lon);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        tmp = Double.doubleToLongBits(lat);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        return 31 * result + time.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s [%f,%s,%s]", time, distance, MathUtils.radian2String(lon), MathUtils.radian2String(lat));
    }

}
