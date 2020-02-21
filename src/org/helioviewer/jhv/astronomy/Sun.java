package org.helioviewer.jhv.astronomy;

import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.JulianDay;
import org.helioviewer.jhv.time.TimeUtils;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

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

    public static final Position StartEarth;

    private static final LoadingCache<JHVTime, Position> carringtonCache = Caffeine.newBuilder().maximumSize(10000)
            .build(Spice::getEarthCarrington);
    private static final LoadingCache<JHVTime, Position> hciCache = Caffeine.newBuilder().maximumSize(10000)
            .build(t -> Spice.getPositionLatitudinal("SUN", "EARTH", Frame.HCI.toString(), t));

    static {
        StartEarth = getEarth(TimeUtils.START);
    }

    public static Position getEarth(JHVTime time) {
        return carringtonCache.get(time);
    }

    public static Position getEarthHCI(JHVTime time) {
        return hciCache.get(time);
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

}
