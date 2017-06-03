package org.helioviewer.jhv.astronomy;

import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.time.JHVDate;

public class Position {

    public static class L {

        public final double rad;
        public final double lon;
        public final double lat;
        public final JHVDate time;

        public L(JHVDate _time, double _rad, double _lon, double _lat) {
            rad = _rad;
            lon = _lon;
            lat = _lat;
            time = _time;
        }

        @Override
        public String toString() {
            return String.format("%s [%f,%f,%f]", time, rad, lon, lat);
        }

    }

    public static class Q {

        public final double distance;
        public final Quat orientation;
        public final JHVDate time;

        public Q(JHVDate _time, double _dist, Quat _q) {
            distance = _dist;
            orientation = _q;
            time = _time;
        }

        @Override
        public String toString() {
            return String.format("%s [%f,%s]", time, distance, orientation);
        }

    }

}
