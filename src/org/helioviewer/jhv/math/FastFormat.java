package org.helioviewer.jhv.math;

public final class FastFormat {

    private static final ThreadLocal<char[]> BUFFER = ThreadLocal.withInitial(() -> new char[32]);

    private static final char[] DIGITS = {
            '0', '0', '0', '1', '0', '2', '0', '3', '0', '4', '0', '5', '0', '6', '0', '7', '0', '8', '0', '9',
            '1', '0', '1', '1', '1', '2', '1', '3', '1', '4', '1', '5', '1', '6', '1', '7', '1', '8', '1', '9',
            '2', '0', '2', '1', '2', '2', '2', '3', '2', '4', '2', '5', '2', '6', '2', '7', '2', '8', '2', '9',
            '3', '0', '3', '1', '3', '2', '3', '3', '3', '4', '3', '5', '3', '6', '3', '7', '3', '8', '3', '9',
            '4', '0', '4', '1', '4', '2', '4', '3', '4', '4', '4', '5', '4', '6', '4', '7', '4', '8', '4', '9',
            '5', '0', '5', '1', '5', '2', '5', '3', '5', '4', '5', '5', '5', '6', '5', '7', '5', '8', '5', '9',
            '6', '0', '6', '1', '6', '2', '6', '3', '6', '4', '6', '5', '6', '6', '6', '7', '6', '8', '6', '9',
            '7', '0', '7', '1', '7', '2', '7', '3', '7', '4', '7', '5', '7', '6', '7', '7', '7', '8', '7', '9',
            '8', '0', '8', '1', '8', '2', '8', '3', '8', '4', '8', '5', '8', '6', '8', '7', '8', '8', '8', '9',
            '9', '0', '9', '1', '9', '2', '9', '3', '9', '4', '9', '5', '9', '6', '9', '7', '9', '8', '9', '9'
    };

    private FastFormat() {}

    public static StringBuilder appendFixed2(StringBuilder out, double value, int width, boolean alwaysSign) {
        return appendFixed(out, value, width, alwaysSign, 100);
    }

    public static String rounded1(double value) {
        return appendRounded(new StringBuilder(8), value, 10).toString();
    }

    public static String rounded2(double value) {
        return appendRounded(new StringBuilder(8), value, 100).toString();
    }

    public static StringBuilder appendFixed3(StringBuilder out, double value, int width, boolean alwaysSign) {
        return appendFixed(out, value, width, alwaysSign, 1000);
    }

    public static StringBuilder appendInteger(StringBuilder out, long value, int width, boolean alwaysSign) {
        if (value == Long.MIN_VALUE)
            return appendPadded(out, Long.toString(value), width);

        boolean negative = value < 0;
        long absValue = negative ? -value : value;

        char[] buf = BUFFER.get();
        int charPos = 32;

        if (absValue == 0) {
            buf[--charPos] = '0';
        } else if (absValue <= Integer.MAX_VALUE) {
            int temp = (int) absValue;
            while (temp >= 100) {
                int q = temp / 100;
                int r = temp - q * 100;
                buf[--charPos] = DIGITS[(r << 1) + 1];
                buf[--charPos] = DIGITS[r << 1];
                temp = q;
            }
            if (temp >= 10) {
                buf[--charPos] = DIGITS[(temp << 1) + 1];
                buf[--charPos] = DIGITS[temp << 1];
            } else {
                buf[--charPos] = (char) ('0' + temp);
            }
        } else {
            long temp = absValue;
            while (temp >= 100) {
                long q = temp / 100;
                int r = (int) (temp - q * 100);
                buf[--charPos] = DIGITS[(r << 1) + 1];
                buf[--charPos] = DIGITS[r << 1];
                temp = q;
            }
            if (temp >= 10) {
                int t = (int) temp;
                buf[--charPos] = DIGITS[(t << 1) + 1];
                buf[--charPos] = DIGITS[t << 1];
            } else {
                buf[--charPos] = (char) ('0' + temp);
            }
        }

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
            out.repeat(' ', spaces);
            out.append(buf, charPos, len);
        }
        return out;
    }

    private static StringBuilder appendRounded(StringBuilder out, double value, int scale) {
        if (!Double.isFinite(value))
            return out.append(value);

        boolean negative = Double.doubleToRawLongBits(value) < 0;
        double absValue = negative ? -value : value;
        if (absValue <= Long.MAX_VALUE && absValue == (long) absValue) {
            if (negative && absValue != 0)
                out.append('-');
            return appendInteger(out, (long) absValue, 0, false);
        }

        double scaledValue = absValue * scale;
        if (scaledValue > Long.MAX_VALUE)
            return out.append(value);

        long scaled = (long) (scaledValue + 0.5 + Math.ulp(scaledValue));
        long whole = scaled / scale;
        long fraction = scaled - whole * scale;
        if (negative && (whole != 0 || fraction != 0))
            out.append('-');
        appendInteger(out, whole, 0, false);

        if (fraction == 0)
            return out;

        out.append('.');
        if (scale == 10) {
            out.append((char) ('0' + fraction));
        } else if (scale == 100) {
            int frac = (int) fraction;
            int d1 = frac / 10;
            int d2 = frac - d1 * 10;
            out.append((char) ('0' + d1));
            if (d2 != 0) {
                out.append((char) ('0' + d2));
            }
        } else {
            while (fraction % 10 == 0) {
                fraction /= 10;
                scale /= 10;
            }
            for (long digit = scale / 10; digit > 0; digit /= 10) {
                out.append((char) ('0' + fraction / digit));
                fraction %= digit;
            }
        }
        return out;
    }

    private static StringBuilder appendFixed(StringBuilder out, double value, int width, boolean alwaysSign, int scale) {
        if (!Double.isFinite(value))
            return appendPadded(out, (alwaysSign && value > 0 ? "+" : "") + value, width);

        boolean negative = Double.doubleToRawLongBits(value) < 0;
        double absValue = negative ? -value : value;
        if (absValue <= Long.MAX_VALUE && absValue == (long) absValue)
            return appendFixed(out, (long) absValue, 0, width, negative, alwaysSign, scale);

        double scaledValue = absValue * scale;
        if (scaledValue > Long.MAX_VALUE)
            return appendPadded(out, (negative ? "-" : alwaysSign ? "+" : "") + absValue, width);

        long scaled = (long) (scaledValue + 0.5 + Math.ulp(scaledValue));
        long whole = scaled / scale;
        long fraction = scaled - whole * scale;
        return appendFixed(out, whole, fraction, width, negative, alwaysSign, scale);
    }

    private static StringBuilder appendFixed(StringBuilder out, long whole, long fraction, int width, boolean negative, boolean alwaysSign, int scale) {
        char[] buf = BUFFER.get();
        int charPos = 32;

        int frac = (int) fraction;
        if (scale == 100) {
            buf[--charPos] = DIGITS[(frac << 1) + 1];
            buf[--charPos] = DIGITS[frac << 1];
        } else {
            int q = frac / 10;
            int r = frac - q * 10;
            buf[--charPos] = (char) ('0' + r);
            buf[--charPos] = DIGITS[(q << 1) + 1];
            buf[--charPos] = DIGITS[q << 1];
        }

        buf[--charPos] = '.';

        if (whole == 0) {
            buf[--charPos] = '0';
        } else if (whole <= Integer.MAX_VALUE) {
            int tempWhole = (int) whole;
            while (tempWhole >= 100) {
                int q = tempWhole / 100;
                int r = tempWhole - q * 100;
                buf[--charPos] = DIGITS[(r << 1) + 1];
                buf[--charPos] = DIGITS[r << 1];
                tempWhole = q;
            }
            if (tempWhole >= 10) {
                buf[--charPos] = DIGITS[(tempWhole << 1) + 1];
                buf[--charPos] = DIGITS[tempWhole << 1];
            } else {
                buf[--charPos] = (char) ('0' + tempWhole);
            }
        } else {
            long tempWhole = whole;
            while (tempWhole >= 100) {
                long q = tempWhole / 100;
                int r = (int) (tempWhole - q * 100);
                buf[--charPos] = DIGITS[(r << 1) + 1];
                buf[--charPos] = DIGITS[r << 1];
                tempWhole = q;
            }
            if (tempWhole >= 10) {
                int t = (int) tempWhole;
                buf[--charPos] = DIGITS[(t << 1) + 1];
                buf[--charPos] = DIGITS[t << 1];
            } else {
                buf[--charPos] = (char) ('0' + tempWhole);
            }
        }

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
            out.repeat(' ', spaces);
            out.append(buf, charPos, len);
        }
        return out;
    }

    private static StringBuilder appendPadded(StringBuilder out, String value, int width) {
        int spaces = width - value.length();
        if (spaces > 0) {
            out.repeat(' ', spaces);
        }
        return out.append(value);
    }
}
