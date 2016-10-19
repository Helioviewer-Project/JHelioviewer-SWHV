package org.helioviewer.jhv.base.conversion;

public class GOESLevelConversion {

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
        double pv = -1;
        if (s.length() >= 2) {
            char v = s.charAt(0);
            double val = Double.parseDouble(s.substring(1));
            if (v == 'A') {
                pv = 1e-8 * val;
            } else if (v == 'B') {
                pv = 1e-7 * val;
            } else if (v == 'C') {
                pv = 1e-6 * val;
            } else if (v == 'M') {
                pv = 1e-5 * val;
            } else if (v == 'X') {
                pv = 1e-4 * val;
            }
        }
        return pv;
    }

}
