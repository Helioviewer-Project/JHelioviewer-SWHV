package org.helioviewer.jhv.astronomy;

import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.JulianDay;

class SSW {

    // derived from http://hesperia.gsfc.nasa.gov/ssw/gen/idl/solar/get_sun.pro
    public static Position getEarthSSW(JHVTime time) {
        double mjd = JulianDay.milli2mjd(time.milli);
        double t = JulianDay.mjd2jcy(mjd, 2415020.);

        // Geometric Mean Longitude (deg)
        double mnl = 279.69668 + 36000.76892 * t + 0.0003025 * t * t;
        // Mean anomaly (deg)
        double mna = 358.47583 + 35999.04975 * t - 0.000150 * t * t - 0.0000033 * t * t * t;
        // Eccentricity of orbit
        double e = 0.01675104 - 0.0000418 * t - 0.000000126 * t * t;
        // Sun's equation of center (deg)
        double c = (1.919460 - 0.004789 * t - 0.000014 * t * t) * Math.sin(mna * MathUtils.degra) + (0.020094 - 0.000100 * t) * Math.sin(2 * mna * MathUtils.degra) + 0.000293 * Math.sin(3 * mna * MathUtils.degra);
        // Sun's true geometric longitude (deg)
        double true_long = mnl + c;
        // Sun's true anomaly (deg):
        double ta = mna + c;
        // Sun's radius vector (AU)
        double dist = 1.0000002 * (1. - e * e) / (1. + e * Math.cos(ta * MathUtils.degra));

        double lamda = true_long - 0.00569; // deg
        double k = 74.3646 + 1.395833 * t; // deg
        double diff = (lamda - k) * MathUtils.degra; // rad
        double i = 7.25 * MathUtils.degra; // rad

        double he_lat = Math.asin(Math.sin(diff) * Math.sin(i)); // rad

        double y = -Math.sin(diff) * Math.cos(i);
        double x = -Math.cos(diff);
        double eta = Math.atan2(y, x); // rad

        double theta = sunRot(mjd); // rad

        double he_lon = (eta - theta) % (2 * Math.PI);
        if (he_lon < 0)
            he_lon += 2 * Math.PI;

        // convert distance to solar radii
        // change L0 Carrington longitude sign to increase towards West, like Stonyhurst
        return new Position(time, dist * Sun.MeanEarthDistance, -he_lon, he_lat);
    }

    private static double sunRot(double mjd) {
        // 1854-01-01.5 / Carrington sidereal period 25.38
        return ((JulianDay.DJM0 - 2398220.) + mjd) * (2 * Math.PI / Carrington.CR_SIDEREAL); // rad
    }

    private static double calculateRotationInRadians(double latitude, double deltaTsec) {
        /*
         * sin2l = sin(latitude)^2 sin4l = sin2l*sin2l rotation =
         * 1.e-6*dt_rot*(2.894-0.428*sin2l-0.37*sin4l)*180/pi.
         *.
         * from rotation rate of small magnetic features (Howard, Harvey, and
         * Forgach, Solar Physics, 130, 295, 1990)
         */

        double sin2l = Math.sin(latitude);
        sin2l *= sin2l;
        double sin4l = sin2l * sin2l;
        return 1.0e-6 * deltaTsec * (2.894 - 0.428 * sin2l - 0.37 * sin4l);
    }

}
