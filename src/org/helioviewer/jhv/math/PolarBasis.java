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

    public static double angle(Vec3 v) {
        // Polar basis: 0 at north, increasing anti-clockwise.
        double theta = Math.atan2(-v.x, v.y);
        theta += 2 * Math.PI;
        theta %= 2 * Math.PI;
        return theta;
    }

    public static Vec3 vec3(double radius, double angleRadians) {
        return new Vec3(x(radius, angleRadians), y(radius, angleRadians), 0);
    }
}
