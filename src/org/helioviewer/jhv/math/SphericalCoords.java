package org.helioviewer.jhv.math;

public final class SphericalCoords {

    private SphericalCoords() {
    }

    public static double radius(Vec3 v) {
        return v.length();
    }

    public static double longitude(Vec3 v) {
        return Math.atan2(v.x, v.z);
    }

    public static double latitude(Vec3 v) {
        double r = radius(v);
        return r == 0 ? 0 : Math.asin(Math.clamp(v.y / r, -1., 1.));
    }

    public static double colatitude(Vec3 v) {
        double r = radius(v);
        return r == 0 ? 0 : Math.acos(Math.clamp(v.y / r, -1., 1.));
    }

    public static double x(double radius, double longitude, double latitude) {
        return radius * Math.cos(latitude) * Math.sin(longitude);
    }

    public static double y(double radius, double longitude, double latitude) {
        return radius * Math.sin(latitude);
    }

    public static double z(double radius, double longitude, double latitude) {
        return radius * Math.cos(latitude) * Math.cos(longitude);
    }

    public static Vec3 vec3(double radius, double longitude, double latitude) {
        return new Vec3(
                x(radius, longitude, latitude),
                y(radius, longitude, latitude),
                z(radius, longitude, latitude));
    }

    public static Vec3 unit(double longitude, double latitude) {
        return vec3(1, longitude, latitude);
    }
}
