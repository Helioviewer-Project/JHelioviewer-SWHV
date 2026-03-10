package org.helioviewer.jhv.math;

public final class PolarBasis {

    private PolarBasis() {
    }

    public static double x(double radius, double angleRadians) {
        return -radius * Math.sin(angleRadians);
    }

    public static double y(double radius, double angleRadians) {
        return radius * Math.cos(angleRadians);
    }

    public static Vec2 vec2(double radius, double angleRadians) {
        // Polar basis: 0 at north, increasing anti-clockwise.
        return new Vec2(x(radius, angleRadians), y(radius, angleRadians));
    }

    public static Vec3 vec3(double radius, double angleRadians) {
        return new Vec3(x(radius, angleRadians), y(radius, angleRadians), 0);
    }
}
