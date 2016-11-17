package org.helioviewer.jhv.base.conversion;

public class GOESLevel {

    public static String getStringValue(double v) {
        if (v < 1e-7)
            return String.format("A%.1f", v * 1e8);
        else if (v < 1e-6)
            return String.format("B%.1f", v * 1e7);
        else if (v < 1e-5)
            return String.format("C%.1f", v * 1e6);
        else if (v < 1e-4)
            return String.format("M%.1f", v * 1e5);
        else
            return String.format("X%.1f", v * 1e4);
    }

    public static double getFloatValue(String s) {
        if (s.length() >= 2) {
            char v = s.charAt(0);

            try {
                double val = Double.parseDouble(s.substring(1));
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
        return -1;
    }

}
