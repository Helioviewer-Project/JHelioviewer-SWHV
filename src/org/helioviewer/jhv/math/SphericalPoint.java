package org.helioviewer.jhv.math;

public record SphericalPoint(double radius, double longitude, double latitude) {

    public static SphericalPoint fromCartesian(Vec3 point) {
        return fromCartesian(point.x, point.y, point.z);
    }

    public static SphericalPoint fromCartesian(double x, double y, double z) {
        return SphericalCoords.point(x, y, z);
    }
}
