package org.helioviewer.jhv.astronomy;

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
    public String toString() {
        return String.format("%s [%f,%f,%f]", time, distance, lon, lat);
    }

}
