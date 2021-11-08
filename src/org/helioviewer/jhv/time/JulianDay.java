package org.helioviewer.jhv.time;

import javax.annotation.Nullable;

// derived from SOFA (http://www.iausofa.org)
public class JulianDay {

    public static final double DJM0 = 2400000.5;
    public static final double UNIX_EPOCH_MJD = (2440587.5 - DJM0);

    public static double milli2mjd(long milli) {
        return UNIX_EPOCH_MJD + milli / (double) TimeUtils.DAY_IN_MILLIS;
    }

    public static double mjd2jcy(double mjd, double epoch) {
        return (DJM0 - epoch + mjd) / 36525.;
    }

    /* Earliest year allowed (4800BC) */
    private static final int IYMIN = -4799;
    /* Month lengths in days */
    private static final int[] mtab = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    public static double cal2mjd(int iy, int im, int id) {
        int my;
        long iypmy;

        /* Validate year and month. */
        if (iy < IYMIN) return -DJM0;
        if (im < 1 || im > 12) return -DJM0;

        /* If February in a leap year, 1, otherwise 0. */
        boolean ly = ((im == 2) && (iy % 4) == 0 && ((iy % 100) != 0 || (iy % 400) == 0));

        /* Validate day, taking into account leap years. */
        if ((id < 1) || (id > (mtab[im - 1] + (ly ? 1 : 0)))) return -DJM0;

        /* Return result. */
        my = (im - 14) / 12;
        iypmy = iy + (long) my;

        return (double) ((1461L * (iypmy + 4800L)) / 4L
                + (367L * (long) (im - 2 - 12 * my)) / 12L
                - (3L * ((iypmy + 4900L) / 100L)) / 4L
                + (long) id - 2432076L);
    }

    /* Minimum and maximum allowed JD */
    private static final double DJMIN = -68569.5;
    private static final double DJMAX = 1e9;

    @Nullable
    public static double[] mjd2cal(double dj2) {
        double id, im, iy, dj1 = DJM0;

        long jd, l, n, i, k;
        double dj, d1, d2, f1, f2, f, d;
        /* Verify date is acceptable. */
        dj = dj1 + dj2;
        if (dj < DJMIN || dj > DJMAX) return null;

        /* Copy the date, big then small, and re-align to midnight. */
        if (dj1 >= dj2) {
            d1 = dj1;
            d2 = dj2;
        } else {
            d1 = dj2;
            d2 = dj1;
        }
        d2 -= 0.5;

        /* Separate day and fraction. */
        f1 = d1 % 1.0;
        f2 = d2 % 1.0;
        f = (f1 + f2) % 1.0;
        if (f < 0.0) f += 1.0;
        d = Math.floor(d1 - f1) + Math.floor(d2 - f2) + Math.floor(f1 + f2 - f);
        jd = (long) Math.floor(d) + 1L;

        /* Express day in Gregorian calendar. */
        l = jd + 68569L;
        n = (4L * l) / 146097L;
        l -= (146097L * n + 3L) / 4L;
        i = (4000L * (l + 1L)) / 1461001L;
        l -= (1461L * i) / 4L - 31L;
        k = (80L * l) / 2447L;
        id = (int) (l - (2447L * k) / 80L);
        l = k / 11L;
        im = (int) (k + 2L - 12L * l);
        iy = (int) (100L * (n - 49L) + i + l);

        return new double[]{iy, im, id, f};
    }

}
