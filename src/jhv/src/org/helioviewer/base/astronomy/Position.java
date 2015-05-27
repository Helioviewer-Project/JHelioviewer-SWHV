package org.helioviewer.base.astronomy;

public class Position {

    public static final class Latitudinal {

        public final double rad;
        public final double lon;
        public final double lat;
        public final long milli;

        public Latitudinal(double _rad, double _lon, double _lat) {
            rad = _rad;
            lon = _lon;
            lat = _lat;
            milli = 0;
        }

        public Latitudinal(long _milli, double _rad, double _lon, double _lat) {
            rad = _rad;
            lon = _lon;
            lat = _lat;
            milli = _milli;
        }

        @Override
        public String toString() {
            return String.format("%d [%f,%f,%f]", milli, rad, lon, lat);
        }

    }

    public static final class Rectangular {

        public final double x;
        public final double y;
        public final double z;
        public final long milli;

        public Rectangular(double _x, double _y, double _z) {
            x = _x;
            y = _y;
            z = _z;
            milli = 0;
        }

        public Rectangular(long _milli, double _x, double _y, double _z) {
            x = _x;
            y = _y;
            z = _z;
            milli = _milli;
        }

        @Override
        public String toString() {
            return String.format("%d [%f,%f,%f]", milli, x, y, z);
        }

    }

}
