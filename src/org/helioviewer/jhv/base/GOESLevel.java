package org.helioviewer.jhv.base;

public class GOESLevel {

    public static String getStringValue(double v) {
        double d;

        d = Math.round(v * 1e9);
        if (d < 1e2)
            return String.format("A%.1f", d * 1e-1);

        d = Math.round(v * 1e8);
        if (d < 1e2)
            return String.format("B%.1f", d * 1e-1);

        d = Math.round(v * 1e7);
        if (d < 1e2)
            return String.format("C%.1f", d * 1e-1);

        d = Math.round(v * 1e6);
        if (d < 1e2)
            return String.format("M%.1f", d * 1e-1);

        return String.format("X%.1f", v * 1e4);
    }

    public static double getFloatValue(String s) {
        if (s.length() >= 2) {
            try {
                double val = Double.parseDouble(s.substring(1));
                char v = s.charAt(0);
                switch (v) {
                    case 'A':
                        return 1e-8 * val;
                    case 'B':
                        return 1e-7 * val;
                    case 'C':
                        return 1e-6 * val;
                    case 'M':
                        return 1e-5 * val;
                    case 'X':
                        return 1e-4 * val;
                    default:
                        break;
                }
            } catch (Exception ignore) {
            }
        }
        return 1; // for log
    }

}
