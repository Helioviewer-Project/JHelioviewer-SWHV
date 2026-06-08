package org.helioviewer.jhv.math;

public final class FastFormat {

    private FastFormat() {}

    public static String text(String value, int width) {
        return appendText(new StringBuilder(Math.max(width, 16)), value, width).toString();
    }

    public static StringBuilder appendFixed2(StringBuilder out, double value, int width, boolean alwaysSign) {
        return appendFixed(out, value, width, alwaysSign, 100);
    }

    public static StringBuilder appendFixed3(StringBuilder out, double value, int width, boolean alwaysSign) {
        return appendFixed(out, value, width, alwaysSign, 1000);
    }

    public static StringBuilder appendInteger(StringBuilder out, long value, int width, boolean alwaysSign) {
        if (value == Long.MIN_VALUE)
            return appendPadded(out, Long.toString(value), width);

        boolean negative = value < 0;
        long absValue = negative ? -value : value;

        char[] buf = new char[32];
        int charPos = 32;

        long temp = absValue;
        while (temp >= 10) {
            long q = temp / 10;
            int r = (int) (temp - q * 10);
            buf[--charPos] = (char) ('0' + r);
            temp = q;
        }
        buf[--charPos] = (char) ('0' + temp);

        if (negative) {
            buf[--charPos] = '-';
        } else if (alwaysSign) {
            buf[--charPos] = '+';
        }

        int len = 32 - charPos;
        if (width <= 32) {
            int spaces = width - len;
            while (spaces > 0) {
                buf[--charPos] = ' ';
                spaces--;
            }
            out.append(buf, charPos, 32 - charPos);
        } else {
            int spaces = width - len;
            out.repeat(" ", spaces);
            out.append(buf, charPos, len);
        }
        return out;
    }

    public static StringBuilder appendText(StringBuilder out, String value, int width) {
        return appendPadded(out, value, width);
    }

    private static StringBuilder appendFixed(StringBuilder out, double value, int width, boolean alwaysSign, int scale) {
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

        char[] buf = new char[32];
        int charPos = 32;

        int frac = (int) fraction;
        if (scale == 100) {
            buf[--charPos] = (char) ('0' + (frac % 10));
            buf[--charPos] = (char) ('0' + (frac / 10));
        } else {
            buf[--charPos] = (char) ('0' + (frac % 10));
            buf[--charPos] = (char) ('0' + ((frac % 100) / 10));
            buf[--charPos] = (char) ('0' + (frac / 100));
        }

        buf[--charPos] = '.';

        long tempWhole = whole;
        while (tempWhole >= 10) {
            long q = tempWhole / 10;
            int r = (int) (tempWhole - q * 10);
            buf[--charPos] = (char) ('0' + r);
            tempWhole = q;
        }
        buf[--charPos] = (char) ('0' + tempWhole);

        if (negative) {
            buf[--charPos] = '-';
        } else if (alwaysSign) {
            buf[--charPos] = '+';
        }

        int len = 32 - charPos;
        if (width <= 32) {
            int spaces = width - len;
            while (spaces > 0) {
                buf[--charPos] = ' ';
                spaces--;
            }
            out.append(buf, charPos, 32 - charPos);
        } else {
            int spaces = width - len;
            out.repeat(" ", spaces);
            out.append(buf, charPos, len);
        }
        return out;
    }

    private static StringBuilder appendPadded(StringBuilder out, String value, int width) {
        int spaces = width - value.length();
        if (spaces > 0) {
            out.repeat(" ", spaces);
        }
        return out.append(value);
    }

}
