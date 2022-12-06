package org.helioviewer.jhv.math;

import java.text.DecimalFormat;

public class MathUtils {

    public static final double radeg = 180 / Math.PI;
    public static final double degra = Math.PI / 180;

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

    // https://stackoverflow.com/questions/11513344/how-to-implement-the-fast-inverse-square-root-in-java
    public static float invSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);
        x = Float.intBitsToFloat(i);
        x *= (1.5f - xhalf * x * x);
        return x;
    }

    public static double invSqrt(double x) {
        double xhalf = 0.5d * x;
        long i = Double.doubleToLongBits(x);
        i = 0x5fe6ec85e7de30daL - (i >> 1);
        x = Double.longBitsToDouble(i);
        x *= (1.5d - xhalf * x * x);
        return x;
    }

    // https://martin.ankerl.com/2012/01/25/optimized-approximative-pow-in-c-and-cpp/
    public static double pow(double a, double b) {
        // exponentiation by squaring
        double r = 1.0;
        int exp = (int) b;
        double base = a;
        while (exp != 0) {
            if ((exp & 1) != 0) {
                r *= base;
            }
            base *= base;
            exp >>= 1;
        }
        // use the IEEE 754 trick for the fraction of the exponent, 1065307417 for float
        double b_faction = b - (int) b;
        long tmp = Double.doubleToLongBits(a);
        long tmp2 = (long) (b_faction * (tmp - 4606921280493453312L)) + 4606921280493453312L;
        return r * Double.longBitsToDouble(tmp2);
    }

}
