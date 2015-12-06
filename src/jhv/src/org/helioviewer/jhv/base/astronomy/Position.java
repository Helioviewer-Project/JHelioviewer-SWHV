package org.helioviewer.jhv.base.astronomy;

import org.helioviewer.jhv.base.math.Quat;

public class Position {

    public static final class Latitudinal {

        public final double rad;
        public final double lon;
        public final double lat;
        public final long milli;

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

    public static final class Quaternional {

        public final double rad;
        public final Quat q;
        public final long milli;

        public Quaternional(long _milli, double _rad, Quat _q) {
            rad = _rad;
            q = _q;
            milli = _milli;
        }

        @Override
        public String toString() {
            return String.format("%d [%f,%s]", milli, rad, q);
        }

    }

    public static final class Rectangular {

        public final double x;
        public final double y;
        public final double z;
        public final long milli;

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
