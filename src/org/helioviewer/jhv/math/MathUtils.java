package org.helioviewer.jhv.math;

import java.text.DecimalFormat;

public class MathUtils {

    private static final double LN_2 = Math.log(2);

    /**
     * Takes and returns the maximum value from the given args.
     *
     * @param _is the values to compare
     * @return the maximum of the given values
     */
    public static int max(int... _is) {
        int max = Integer.MIN_VALUE;
        for (int i : _is)
            if (max < i)
                max = i;
        return max;
    }

    /**
     * Takes and returns the minimum value from the given args.
     *
     * @param _is the values to compare
     * @return the minimum of the given values
     */
    public static int min(int... _is) {
        int min = Integer.MAX_VALUE;
        for (int i : _is)
            if (min > i)
                min = i;
        return min;
    }

    public static double mapTo0To360(double x) {
        x %= 360.;
        if (x < 0)
            x += 360.;
        return x;
    }

    public static double mapToMinus180To180(double x) {
        x %= 360.;
        if (x > 180.)
            x -= 360.;
        else if (x < -180.)
            x += 360;
        return x;
    }

    public static DecimalFormat numberFormatter(String pattern, int maxDigits) {
        DecimalFormat f = new DecimalFormat(pattern /*, DecimalFormatSymbols.getInstance(Locale.ENGLISH)*/);
        f.setMaximumFractionDigits(maxDigits);
        return f;
    }

    public static int roundDownTo(int a, int quanta) { // works with pot quanta
        return a & -quanta;
    }

    public static int roundUpTo(int a, int quanta) { // works with pot quanta
        return (a + (quanta - 1)) & -quanta;
    }

    public static double asinh(double x) {
        if (x < 0)
            return -asinh(-x);
        if (x > 1e8)
            return Math.log(x) + LN_2;
        return Math.log(x + Math.sqrt(x * x + 1));
    }

}
