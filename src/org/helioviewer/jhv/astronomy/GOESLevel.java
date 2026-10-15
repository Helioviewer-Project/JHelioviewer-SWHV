package org.helioviewer.jhv.astronomy;

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
        String value = s.trim();
        if (value.length() < 2)
            throw new IllegalArgumentException("Invalid GOES class: " + s);

        double scale = switch (Character.toUpperCase(value.charAt(0))) {
            case 'A' -> 1e-8;
            case 'B' -> 1e-7;
            case 'C' -> 1e-6;
            case 'M' -> 1e-5;
            case 'X' -> 1e-4;
            default -> throw new IllegalArgumentException("Invalid GOES class: " + s);
        };
        double magnitude = Double.parseDouble(value.substring(1));
        if (!Double.isFinite(magnitude) || magnitude < 0)
            throw new IllegalArgumentException("Invalid GOES class: " + s);
        return scale * magnitude;
    }

}
