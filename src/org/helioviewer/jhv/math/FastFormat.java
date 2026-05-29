package org.helioviewer.jhv.math;

public final class FastFormat {

    private FastFormat() {}

    public static String fixed2(double value, int width, boolean alwaysSign) {
        return appendFixed2(new StringBuilder(Math.max(width, 16)), value, width, alwaysSign).toString();
    }

    public static String fixed3(double value, int width, boolean alwaysSign) {
        return appendFixed3(new StringBuilder(Math.max(width, 16)), value, width, alwaysSign).toString();
    }

    public static String integer(long value, int width, boolean alwaysSign) {
        return appendInteger(new StringBuilder(Math.max(width, 16)), value, width, alwaysSign).toString();
    }

    public static String text(String value, int width) {
        return appendText(new StringBuilder(Math.max(width, 16)), value, width).toString();
    }

    public static StringBuilder appendFixed2(StringBuilder out, double value, int width, boolean alwaysSign) {
        return appendFixed(out, value, width, alwaysSign, 100, 10);
    }

    public static StringBuilder appendFixed3(StringBuilder out, double value, int width, boolean alwaysSign) {
        return appendFixed(out, value, width, alwaysSign, 1000, 100);
    }

    public static StringBuilder appendInteger(StringBuilder out, long value, int width, boolean alwaysSign) {
        if (value == Long.MIN_VALUE)
            return appendPadded(out, Long.toString(value), width);

        boolean negative = value < 0;
        long absValue = negative ? -value : value;
        int len = digitCount(absValue);
        if (negative || alwaysSign)
            len++;

        appendSpaces(out, width - len);
        if (negative)
            out.append('-');
        else if (alwaysSign)
            out.append('+');
        appendUnsigned(out, absValue);
        return out;
    }

    public static StringBuilder appendText(StringBuilder out, String value, int width) {
        return appendPadded(out, value, width);
    }

    private static StringBuilder appendFixed(StringBuilder out, double value, int width, boolean alwaysSign, int scale, int firstFractionDivisor) {
        if (!Double.isFinite(value))
            return appendPadded(out, (alwaysSign && value > 0 ? "+" : "") + value, width);

        boolean negative = Double.doubleToRawLongBits(value) < 0;
        double absValue = negative ? -value : value;
        double scaledValue = absValue * scale;
        if (scaledValue > Long.MAX_VALUE)
            return appendPadded(out, (negative ? "-" : alwaysSign ? "+" : "") + absValue, width);

        long scaled = (long) Math.floor(scaledValue + 0.5 + Math.ulp(scaledValue));
        long whole = scaled / scale;
        long fraction = scaled - whole * scale;
        int precision = scale == 100 ? 2 : 3;
        int len = digitCount(whole) + 1 + precision;
        if (negative || alwaysSign)
            len++;

        appendSpaces(out, width - len);
        if (negative)
            out.append('-');
        else if (alwaysSign)
            out.append('+');
        appendUnsigned(out, whole);
        out.append('.');
        appendFraction(out, fraction, firstFractionDivisor);
        return out;
    }

    private static StringBuilder appendPadded(StringBuilder out, String value, int width) {
        appendSpaces(out, width - value.length());
        return out.append(value);
    }

    private static void appendSpaces(StringBuilder out, int count) {
        out.repeat(" ", Math.max(0, count));
    }

    private static void appendUnsigned(StringBuilder out, long value) {
        long divisor = 1;
        while (divisor <= Long.MAX_VALUE / 10 && value / divisor >= 10)
            divisor *= 10;
        while (divisor != 0) {
            long digit = value / divisor;
            out.append((char) ('0' + (int) digit));
            value -= digit * divisor;
            divisor /= 10;
        }
    }

    private static void appendFraction(StringBuilder out, long value, int divisor) {
        while (divisor != 0) {
            long digit = value / divisor;
            out.append((char) ('0' + (int) digit));
            value -= digit * divisor;
            divisor /= 10;
        }
    }

    private static int digitCount(long value) {
        int digits = 1;
        while (value >= 10) {
            value /= 10;
            digits++;
        }
        return digits;
    }

}
