package org.helioviewer.jhv.position;

import java.util.Objects;

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
    public boolean equals(Object o) {
        if (!(o instanceof Position))
            return false;
        Position p = (Position) o;
        return distance == p.distance && lon == p.lon && lat == p.lat && time.equals(p.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distance, lon, lat, time);
    }

    @Override
    public String toString() {
        return String.format("%s [%f,%f,%f]", time, distance, lon, lat);
    }

}
