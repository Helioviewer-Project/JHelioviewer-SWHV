package org.helioviewer.jhv.event;

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
                return switch (v) {
                    case 'A' -> 1e-8 * val;
                    case 'B' -> 1e-7 * val;
                    case 'C' -> 1e-6 * val;
                    case 'M' -> 1e-5 * val;
                    case 'X' -> 1e-4 * val;
                    default -> 1.0;
                };
            } catch (Exception ignore) {}
        }
        return 1; // for log
    }

}
