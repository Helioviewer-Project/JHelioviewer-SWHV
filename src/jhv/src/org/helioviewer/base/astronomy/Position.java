package org.helioviewer.base.astronomy;

public class Position {

    public static final class Latitudinal {

        public final double rad;
        public final double lon;
        public final double lat;

        public Latitudinal(double _rad, double _lon, double _lat) {
            rad = _rad;
            lon = _lon;
            lat = _lat;
        }

    }

    public static final class Rectangular {

        public final double x;
        public final double y;
        public final double z;

        public Rectangular(double _x, double _y, double _z) {
            x = _x;
            y = _y;
            z = _z;
        }

    }

}
