package org.helioviewer.jhv.astronomy;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.JulianDay;
import org.helioviewer.jhv.time.TimeUtils;

public class Carrington {

    public static final double CR_SIDEREAL = 25.38; // days
    public static final double CR_SYNODIC_MEAN = 27.2753;

    public static final int CR_MINIMAL = 1557;
    public static final int CR_MAXIMAL = 2627;

    public static final long[] CR_start = CarringtonData.CR_START;

    // derived from tim2carr
    public static double time2CR(JHVTime time) {
        double mjd = JulianDay.milli2mjd(time.milli);
        double cr = ((JulianDay.DJM0 - 2398167.) + mjd) / CR_SYNODIC_MEAN + 1.;
        int icr = (int) cr;
        double fcr = cr - icr;
        double flon = Sun.getEarth(time).lon / (2 * Math.PI);

        if (Math.abs(fcr - flon) > 12 / 360.) {
            if (fcr > flon)
                icr++;
            else if (fcr < flon)
                icr--;
        }
        return flon + icr;
    }

    public static void computeTable() {
        JHVTime next = TimeUtils.MINIMAL_TIME;

        while (next.milli < TimeUtils.MAXIMAL_TIME.milli) {
            double cr = time2CR(next);
            long delta_next = (long) ((CR_SYNODIC_MEAN - 0.25) * TimeUtils.DAY_IN_MILLIS);

            BigDecimal bd = new BigDecimal(cr).setScale(6, RoundingMode.HALF_EVEN);
            double rcr = bd.doubleValue();
            if (rcr == (int) rcr) {
                System.out.println("        /* " + ((int) rcr) + " */ " + next.milli + "L, /* " + next + " */");
                next = new JHVTime(next.milli + delta_next);
            } else
                next = new JHVTime(next.milli + 500);
        }
    }

}
