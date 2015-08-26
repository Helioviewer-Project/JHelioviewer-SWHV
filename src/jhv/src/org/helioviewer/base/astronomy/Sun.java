package org.helioviewer.base.astronomy;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.base.math.MathUtils;
import org.helioviewer.base.time.JulianDay;

import org.apache.commons.lang3.time.DateUtils;

public class Sun {

    public static final double Radius = 1;
    public static final double Radius2 = Radius * Radius;
    public static final double RadiusMeter = 6.96e8;

    public static final double MeanEarthDistanceMeter = 149597870700.0;
    public static final double MeanEarthDistance = (MeanEarthDistanceMeter / RadiusMeter);

    private static final Calendar calendar = new GregorianCalendar();

    private static double date2mjd(Date date) {
        calendar.setTime(date);
        int y = calendar.get(Calendar.YEAR);
        int m = calendar.get(Calendar.MONTH) + 1;
        int d = calendar.get(Calendar.DATE);
        double f = DateUtils.getFragmentInMilliseconds(calendar, Calendar.DATE) / (double) DateUtils.MILLIS_PER_DAY;

        return JulianDay.cal2mjd(y, m, d) + f;
    }

    private static double mjd2jcy(double mjd, double epoch) {
        return (JulianDay.DJM0 - epoch + mjd) / 36525.;
    }

    private static Position.Latitudinal prevEarth = new Position.Latitudinal(0, 0, 0, 0);

    // derived from http://hesperia.gsfc.nasa.gov/ssw/gen/idl/solar/get_sun.pro
    public static Position.Latitudinal getEarth(Date date) {
        long milli;
        if ((milli = date.getTime()) == prevEarth.milli) {
            return prevEarth;
        }

        double mjd = date2mjd(date);
        double t = mjd2jcy(mjd, 2415020.);

        // Geometric Mean Longitude (deg)
        double mnl = 279.69668 + 36000.76892 * t + 0.0003025 * t * t;
        // Mean anomaly (deg)
        double mna = 358.47583 + 35999.04975 * t - 0.000150 * t * t - 0.0000033 * t * t * t;
        // Eccentricity of orbit
        double e = 0.01675104 - 0.0000418 * t - 0.000000126 * t * t;
        // Sun's equation of center (deg)
        double c = (1.919460 - 0.004789 * t - 0.000014 * t * t) * Math.sin(mna / MathUtils.radeg) + (0.020094 - 0.000100 * t) * Math.sin(2 * mna / MathUtils.radeg) + 0.000293 * Math.sin(3 * mna / MathUtils.radeg);
        // Sun's true geometric longitude (deg)
        double true_long = mnl + c;
        // Sun's true anomaly (deg):
        double ta = mna + c;
        // Sun's radius vector (AU)
        double dist = 1.0000002 * (1. - e * e) / (1. + e * Math.cos(ta / MathUtils.radeg));

        double lamda = true_long - 0.00569; // deg
        double k = 74.3646 + 1.395833 * t; // deg
        double diff = (lamda - k) / MathUtils.radeg; // rad
        double i = 7.25 / MathUtils.radeg; // rad

        double he_lat = Math.asin(Math.sin(diff) * Math.sin(i)); // rad

        double y = -Math.sin(diff) * Math.cos(i);
        double x = -Math.cos(diff);
        double eta = Math.atan2(y, x); // rad

        // 1854-01-01.5 / Carrington sidereal period 25.38
        double theta = ((JulianDay.DJM0 - 2398220.) + mjd) * (2 * Math.PI / 25.38); // rad

        double he_lon = (eta - theta) % (2 * Math.PI);
        if (he_lon < 0)
            he_lon += (2 * Math.PI);

        // convert distance to solar radii
        // change L0 Carrington longitude sign to increase towards West, like Stonyhurst
        Position.Latitudinal Earth = new Position.Latitudinal(milli, dist * Sun.MeanEarthDistance, -he_lon, he_lat);
        prevEarth = Earth;

        return Earth;
    }

    // better precison, to be recovered later
    private static double getL0Degree(Date date) {
        double mjd = date2mjd(date);
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

        y = -Math.sin(diff) * Math.cos(i / MathUtils.radeg);
        x = -Math.cos(diff);
        double eta = Math.atan2(y, x) * MathUtils.radeg + 360.;
        double long0 = (eta - theta) % 360. + 360.;
        return long0;
    }

    public static double calculateRotationInRadians(double latitude, double deltaTsec) {
        /*
         * sin2l = sin(latitude)^2 sin4l = sin2l*sin2l rotation =
         * 1.e-6*dt_rot*(2.894-0.428*sin2l-0.37*sin4l)*180/pi.
         *.
         * from rotation rate of small magnetic features (Howard, Harvey, and
         * Forgach, Solar Physics, 130, 295, 1990)
         */

        double sin2l = Math.sin(latitude);
        sin2l = sin2l * sin2l;
        double sin4l = sin2l * sin2l;
        return 1.0e-6 * deltaTsec * (2.894 - 0.428 * sin2l - 0.37 * sin4l);
    }

}
