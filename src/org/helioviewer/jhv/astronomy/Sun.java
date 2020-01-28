package org.helioviewer.jhv.astronomy;

import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.JulianDay;
import org.helioviewer.jhv.time.TimeUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class Sun {

    public static final double CLIGHT = 299792458;

    // http://asa.usno.navy.mil/static/files/2016/Astronomical_Constants_2016.pdf
    private static final double SunEarthMassRatio = 332946.0487;
    // https://en.wikipedia.org/wiki/Lagrangian_point#L1
    public static final double L1Factor = 1 - Math.cbrt(1 / SunEarthMassRatio / 3);

    public static final double Radius = 1;
    public static final double Radius2 = Radius * Radius;
    public static final double RadiusMeter = 695508 * 1e3; // photospheric: Allen, SolO
    public static final double RadiusKMeterInv = 1 / 695508.;
    public static final double RadiusMilli = RadiusMeter / CLIGHT * 1e3;

    private static final double MeanEarthDistanceMeter = 149597870.7 * 1e3;
    public static final double MeanEarthDistance = MeanEarthDistanceMeter / RadiusMeter;
    public static final double MeanEarthDistanceInv = RadiusMeter / MeanEarthDistanceMeter;

    // doi:10.1088/0004-637X/812/2/91
    public static final double RadiusFactor_171 = 7.01 * 1e5 * 1e3 / RadiusMeter; // Frédéric Auchère, personal communication
    public static final double RadiusFactor_304 = 7.01 * 1e5 * 1e3 / RadiusMeter; // for visualisation better use same
    // public static final double RadiusFactor_171 = MeanEarthDistance * Math.tan(969.54 / 3600 * Math.PI / 180);
    // public static final double RadiusFactor_304 = MeanEarthDistance * Math.tan(967.56 / 3600 * Math.PI / 180);
    public static final double RadiusFactor_1600 = MeanEarthDistance * Math.tan(963.04 / 3600 * Math.PI / 180);
    public static final double RadiusFactor_1700 = MeanEarthDistance * Math.tan(961.76 / 3600 * Math.PI / 180);
    public static final double RadiusFactor_5000 = MeanEarthDistance * Math.tan(959.63 / 3600 * Math.PI / 180);
    public static final double RadiusFactor_6173 = MeanEarthDistance * Math.tan(959.57 / 3600 * Math.PI / 180);
    public static final double RadiusFactor_6562 = MeanEarthDistance * Math.tan(960.017 / 3600 * Math.PI / 180);

    private static final JHVTime EPOCH = new JHVTime("2000-01-01T00:00:00");
    private static final Position EpochEarth;
    public static final Position StartEarth;

    private static final LoadingCache<JHVTime, Position> cache = CacheBuilder.newBuilder().maximumSize(10000).build(CacheLoader.from(Sun::getEarthInternal));

    static {
        EpochEarth = getEarth(EPOCH);
        StartEarth = getEarth(TimeUtils.START);
    }

    public static Position getEarth(JHVTime time) {
        return cache.getUnchecked(time);
    }

    // derived from http://hesperia.gsfc.nasa.gov/ssw/gen/idl/solar/get_sun.pro
    private static Position getEarthInternal(JHVTime time) {
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
        return new Position(time, dist * MeanEarthDistance, -he_lon, he_lat);
    }

    public static double getEarthDistance(long milli) {
        double mjd = JulianDay.milli2mjd(milli);
        double t = JulianDay.mjd2jcy(mjd, 2415020.);

        // Geometric Mean Longitude (deg)
        // double mnl = 279.69668 + 36000.76892 * t + 0.0003025 * t * t;
        // Mean anomaly (deg)
        double mna = 358.47583 + 35999.04975 * t - 0.000150 * t * t - 0.0000033 * t * t * t;
        // Eccentricity of orbit
        double e = 0.01675104 - 0.0000418 * t - 0.000000126 * t * t;
        // Sun's equation of center (deg)
        double c = (1.919460 - 0.004789 * t - 0.000014 * t * t) * Math.sin(mna * MathUtils.degra) + (0.020094 - 0.000100 * t) * Math.sin(2 * mna * MathUtils.degra) + 0.000293 * Math.sin(3 * mna * MathUtils.degra);
        // Sun's true geometric longitude (deg)
        // double true_long = mnl + c;
        // Sun's true anomaly (deg):
        double ta = mna + c;
        // Sun's radius vector (AU)
        double dist = 1.0000002 * (1. - e * e) / (1. + e * Math.cos(ta * MathUtils.degra));

        return dist * MeanEarthDistance;
    }

    private static double sunRot(double mjd) {
        // 1854-01-01.5 / Carrington sidereal period 25.38
        return ((JulianDay.DJM0 - 2398220.) + mjd) * (2 * Math.PI / Carrington.CR_SIDEREAL); // rad
    }

    private static final double theta0 = sunRot(JulianDay.milli2mjd(EPOCH.milli));

    public static double getHCILongitude(JHVTime time) {
        // 1.7381339560109783
        return sunRot(JulianDay.milli2mjd(time.milli)) + (1.738033457804639 + EpochEarth.lon - theta0);
    }

    public static Quat getHCI(JHVTime time) {
        return new Quat(0, getHCILongitude(time));
    }

    // better precison, to be recovered later
/*
    private static double getL0Degree(long milli) {
        double mjd = milli2mjd(milli);
        double t = mjd2jcy(mjd, 2451545.);

        double mnl = 280.46645 + 36000.76983 * t + 0.0003032 * t * t;
        mnl = mnl % 360.;
        double mna = 357.52910 + 35999.05030 * t - 0.0001559 * t * t - 0.0000048 * t * t * t;
        mna = mna % 360.;

        double c = (1.914600 - 0.004817 * t - 0.000014 * t * t) * Math.sin(mna / MathUtils.radeg) + (0.019993 - 0.000101 * t) * Math.sin(2 * mna / MathUtils.radeg) + 0.000290 * Math.sin(3 * mna / MathUtils.radeg);
        double true_long = (mnl + c) % 360.;
        double omega = 125.04 - 1934.136 * t;
        double ob1 = 23.4392991 - 0.01300417 * t - 0.00059 * t * t / 3600. + 0.001813 * t * t * t / 3600.;

        double ob1tom = 125.04452 - 1934.136261 * t;
        double Lt = 280.4665 + 36000.7698 * t;
        double Lpt = 218.3165 + 481267.8813 * t;
        double ob1t = ob1 + 9.2 / 3600. * Math.cos(ob1tom / MathUtils.radeg) + 0.57 / 3600. * Math.cos(2 * Lt / MathUtils.radeg) + 0.1 / 3600. * Math.cos(2 * Lpt / MathUtils.radeg) - 0.09 / 3600. * Math.cos(2 * ob1tom / MathUtils.radeg);
        //double deps = 9.2 / 3600. * Math.cos(ob1tom) + 0.57 / 3600. * Math.cos(2 * Lt) + 0.1 / 3600. * Math.cos(2 * Lpt) - 0.09 / 3600. * Math.cos(2 * ob1tom);
        double theta = ((JulianDay.DJM0 - 2398220.) + mjd) * 360. / 25.38;
        double k = 73.6667 + 1.3958333 * mjd2jcy(mjd, 2396758.);
        double i = 7.25;
        double lamda = true_long - 0.005705;
        double lamda2 = lamda - 0.00478 * Math.sin(omega / MathUtils.radeg);
        double diff = (lamda - k) / MathUtils.radeg;
        double x = Math.atan(-Math.cos(lamda2 / MathUtils.radeg) * Math.tan(ob1t / MathUtils.radeg)) * MathUtils.radeg;
        double y = Math.atan(-Math.cos(diff) * Math.tan(i / MathUtils.radeg)) * MathUtils.radeg;

        //y = -Math.sin(diff) * Math.cos(i / MathUtils.radeg);
        //x = -Math.cos(diff);
        double eta = Math.atan2(y, x) * MathUtils.radeg + 360.;
        double long0 = (eta - theta) % 360. + 360.;
        return long0;
    }
*/

    public static double calculateRotationInRadians(double latitude, double deltaTsec) {
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
