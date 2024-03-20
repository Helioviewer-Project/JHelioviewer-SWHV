package org.helioviewer.jhv.astronomy;

import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.time.JHVTime;

public class Position {

    public record Cartesian(long milli, double x, double y, double z) {
    }

    public final double distance;
    public final double lon;
    public final double lat;
    public final JHVTime time;
    private Quat q;

    public Position(JHVTime _time, double _distance, double _lon, double _lat) {
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
        if (o instanceof Position p)
            return Double.doubleToLongBits(distance) == Double.doubleToLongBits(p.distance) &&
                    Double.doubleToLongBits(lon) == Double.doubleToLongBits(p.lon) &&
                    Double.doubleToLongBits(lat) == Double.doubleToLongBits(p.lat) &&
                    time.equals(p.time);
        return false;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(distance);
        result = 31 * result + Double.hashCode(lon);
        result = 31 * result + Double.hashCode(lat);
        return 31 * result + time.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s [%f,%s,%s]", time, distance, Math.toDegrees(lon), Math.toDegrees(lat));
    }

}
