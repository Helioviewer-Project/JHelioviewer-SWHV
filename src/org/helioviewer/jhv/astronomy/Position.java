package org.helioviewer.jhv.astronomy;

import org.helioviewer.jhv.math.Quat;
//import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVDate;
//import org.helioviewer.jhv.time.TimeUtils;

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

        public Q toQ() {
            return new Position.Q(time, rad, toQuat());
        }

        public Quat toQuat() {
            return new Quat(lat, lon);
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
//        private JHVDate timeSun;

        public Q(JHVDate _time, double _dist, Quat _q) {
            distance = _dist;
            orientation = _q;
            time = _time;
        }
/*
        public Q sunAxis() {
            return new Q(time, distance, orientation.twist(Vec3.YAxis));
        }

        public JHVDate getTimeSun() {
            if (timeSun == null)
                timeSun = new JHVDate(TimeUtils.floorSec(time.milli - (long) (distance * Sun.RadiusMilli + .5)));
            return timeSun;
        }
*/
        @Override
        public String toString() {
            return String.format("%s [%f,%s]", time, distance, orientation);
        }

    }

}
