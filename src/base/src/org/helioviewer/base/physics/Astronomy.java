package org.helioviewer.base.physics;

import java.util.Calendar;

import org.helioviewer.base.math.MathUtils;

public class Astronomy {

    // This method is based on the SolarSoft GET_SUN routine
    public static double getB0InRadians(Calendar time) {
        Calendar item = time;

        Calendar itemx = item;

        JulianDay jd = new JulianDay(itemx);

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

}
