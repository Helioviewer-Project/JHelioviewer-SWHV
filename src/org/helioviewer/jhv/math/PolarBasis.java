package org.helioviewer.jhv.math;

public final class PolarBasis {

    private PolarBasis() {
    }

    public static Vec2 vec2(double radius, double angleRadians) {
        // Polar basis: 0 at north, increasing anti-clockwise.
        return new Vec2(-radius * Math.sin(angleRadians), radius * Math.cos(angleRadians));
    }

    public static Vec3 vec3(double radius, double angleRadians) {
        Vec2 v = vec2(radius, angleRadians);
        return new Vec3(v.x, v.y, 0);
    }
}
