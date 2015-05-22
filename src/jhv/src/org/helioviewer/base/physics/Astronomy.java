package org.helioviewer.base.physics;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.math.MathUtils;

public class Astronomy {
    private static final Calendar calendar = new GregorianCalendar();

    public static double getB0Degree(Date date) {
        calendar.setTime(date);
        return (getB0InRadians(calendar)) * MathUtils.radeg;
    }

    public static double getB0Radians(Date date) {
        calendar.setTime(date);
        return getB0InRadians(calendar);
    }

    public static double getDistanceSolarRadii(Date date) {
        calendar.setTime(date);
        return getDistance(calendar) * Sun.MeanEarthDistance;
    }

    private static double getDistance(Calendar time) {
        JulianDay jd = new JulianDay(time);
        double t = (jd.getJDN() - 2415020) / 36525;
        double L0 = 280.46645 + 36000.76983 * t + 0.0003032 * t * t;
        L0 = L0 % 360.;
        double M = 357.52910 + 35999.05030 * t - 0.0001559 * t * t - 0.0000048 * t * t * t;
        M = M % 360.;
        double e = 0.0167908617 + t * 0.000042037 + t * t * 0.0000001236;
        double C = (1.914600 * t + 0.004817 * t + 0.000014 * t * t) * Math.sin(M / MathUtils.radeg) + (0.019993 - 0.000101 * t) * Math.sin(2. * M / MathUtils.radeg) + 0.000290 * Math.sin(3. * M / MathUtils.radeg);
        return 1.000001018 * (1. - e * e) / (1. + e * Math.cos((M + C) / MathUtils.radeg));
    }

    public static double getB0InRadians(Calendar time) {
        JulianDay jd = new JulianDay(time);

        double t = (jd.getJDN() - 2415020) / 36525;

        double mnl = 279.69668 + 36000.76892 * t + 0.0003025 * t * t;
        mnl = MathUtils.mapTo0To360(mnl);

        double mna = 358.47583 + 35999.04975 * t - 0.000150 * t * t - 0.0000033 * t * t * t;
        mna = MathUtils.mapTo0To360(mna);

        double c = (1.919460 - 0.004789 * t - 0.000014 * t * t) * Math.sin(mna / MathUtils.radeg) + (0.020094 - 0.000100 * t) * Math.sin(2 * mna / MathUtils.radeg) + 0.000293 * Math.sin(3 * mna / MathUtils.radeg);

        double true_long = MathUtils.mapTo0To360(mnl + c);

        double k = 74.3646 + 1.395833 * t;

        double lamda = true_long - 0.00569;

        double diff = (lamda - k) / MathUtils.radeg;

        // do we want to change this to 7.33?
        double i = 7.25;

        double he_lat = Math.asin(Math.sin(diff) * Math.sin(i / MathUtils.radeg));

        return he_lat;
    }

    public static double getB0InDegree(Calendar time) {
        return getB0InRadians(time) * MathUtils.radeg;
    }

    public static double ymd2jd(int y, int m, int d) {
        double jd = 367 * y - 7 * (y + (m + 9) / 12) / 4 - 3 * ((y + (m - 9) / 7) / 100 + 1) / 4 + 275 * m / 9 + d + 1721029. - 0.5;
        return jd;
    }

    public static double getL0Degree(int year, int month, int day, double et) {
        double jd = ymd2jd(year, month, day) + et / 24.;
        double t = (jd - 2451545.) / 36525.;
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
        double theta = (jd - 2398220.) * 360. / 25.38;
        double k = 73.6667 + 1.3958333 * (jd - 2396758.) / 36525.;
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

    public static double getL0Radians(Date date) {
        calendar.setTime(date);
        int nosecs = calendar.get(Calendar.HOUR_OF_DAY) * 60 * 60 + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND);
        return -Math.PI / 180. * getL0Degree(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), (nosecs) / 60. / 60.);
    }

    public static double getL0Degree(Date date) {
        calendar.setTime(date);
        int nosecs = calendar.get(Calendar.HOUR_OF_DAY) * 60 * 60 + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND);
        return getL0Degree(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), (nosecs) / 60. / 60.);
    }
}
