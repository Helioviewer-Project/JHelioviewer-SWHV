package org.helioviewer.jhv.math;

import java.text.DecimalFormat;

public class MathUtils {

    public static final double radeg = 180 / Math.PI;
    public static final double degra = Math.PI / 180;

    public static String radian2String(double r) {
        double d = Math.toDegrees(r);
        double f = d % 1;
        int id = (int) (d - f);

        double m = Math.abs(f * 60.);
        f = m % 1;
        int im = (int) (m - f);

        return String.format("%d\u00B0%d\u2032%.2f\u2033", id, im, f * 60.);
    }

    /**
     * Returns the integer, x, closest on the number line such that
     * min(_side1,_side2) <= x <= max(_side1,_side2).
     *
     * @param _val   the value to squeee into the interval
     * @param _side1 one side of the interval
     * @param _side2 the other side of the interval
     * @return the closest value within the interval
     */
    public static int squeezeToInterval(int _val, int _side1, int _side2) {
        int temp = Math.max(_side1, _side2);
        _side1 = Math.min(_side1, _side2);
        _side2 = temp;
        if (_val <= _side1)
            return _side1;
        else if (_val >= _side2)
            return _side2;
        else
            return _val;
    }

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

    public static int clip(int val, int from, int to) {
        return val < from ? from : (val > to ? to : val);
    }

    public static long clip(long val, long from, long to) {
        return val < from ? from : (val > to ? to : val);
    }

    public static float clip(float val, float from, float to) {
        return val < from ? from : (val > to ? to : val);
    }

    public static double clip(double val, double from, double to) {
        return val < from ? from : (val > to ? to : val);
    }

    public static int clip(int val, int max) {
        return val < max ? val : max;
    }

    public static int nextPowerOfTwo(int n) {
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        n++;
        return n;
    }

    public static int roundDownTo(int a, int quanta) { // works with pot quanta
        return a & -quanta;
    }

    public static int roundUpTo(int a, int quanta) { // works with pot quanta
        return (a + (quanta - 1)) & -quanta;
    }

}
