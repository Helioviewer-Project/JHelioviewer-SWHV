package org.helioviewer.jhv.math;

public final class SphericalCoords {

    public static double radius(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static double longitude(Vec3 v) {
        return longitude(v.x, v.z);
    }

    public static double longitude(double x, double z) {
        return Math.atan2(x, z);
    }

    public static double latitude(Vec3 v) {
        return latitude(v.x, v.y, v.z);
    }

    public static double latitude(double x, double y, double z) {
        double radius = radius(x, y, z);
        return radius == 0 ? 0 : Math.asin(Math.clamp(y / radius, -1., 1.));
    }

    // public static double colatitude(Vec3 v) {
    //    double r = radius(v);
    //    return r == 0 ? 0 : Math.acos(Math.clamp(v.y / r, -1., 1.));
    // }

    public static double x(double radius, double longitude, double latitude) {
        return radius * Math.cos(latitude) * Math.sin(longitude);
    }

    public static double y(double radius, double latitude) {
        return radius * Math.sin(latitude);
    }

    public static double z(double radius, double longitude, double latitude) {
        return radius * Math.cos(latitude) * Math.cos(longitude);
    }

    static SphericalPoint point(double x, double y, double z) {
        double radius = radius(x, y, z);
        return new SphericalPoint(
                radius,
                longitude(x, z),
                radius == 0 ? 0 : Math.asin(Math.clamp(y / radius, -1., 1.)));
    }

    public static Vec3 vec3(double radius, double longitude, double latitude) {
        double cosLat = Math.cos(latitude);
        return new Vec3(
                radius * cosLat * Math.sin(longitude),
                y(radius, latitude),
                radius * cosLat * Math.cos(longitude));
    }

    public static Vec3 unit(double longitude, double latitude) {
        return vec3(1, longitude, latitude);
    }

    private SphericalCoords() {}
}
