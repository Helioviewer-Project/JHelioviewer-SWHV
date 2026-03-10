package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;

final class NonOrthoProjection {

    private NonOrthoProjection() {
    }

    // See docs/non-ortho-projection-note.md for the shared Java/GLSL convention.
    static Vec2 projectLatitudinal(Position viewpoint, GridType gridType, Vec3 v, GridScale scale) {
        return projectLatitudinal(mapRotation(gridType, viewpoint).rotateVector(v), scale);
    }

    static Vec3 unprojectLatitudinal(Position viewpoint, GridType gridType, Vec2 pt) {
        return mapRotation(gridType, viewpoint).rotateInverseVector(unprojectLatitudinal(pt));
    }

    static Vec2 projectPolar(Position viewpoint, GridType gridType, Vec3 v, GridScale scale) {
        return projectPolar(mapRotation(gridType, viewpoint).rotateVector(v), scale);
    }

    static Vec3 unprojectPolar(Position viewpoint, GridType gridType, Vec2 pt) {
        return mapRotation(gridType, viewpoint).rotateInverseVector(unprojectPolar(pt));
    }

    private static Quat mapRotation(GridType gridType, Position viewpoint) {
        // Non-ortho maps use the same longitude as GridType.toGrid(), but the reflected
        // flat-map basis makes the effective viewpoint latitude rotation positive.
        return Quat.createXY(gridType == GridType.Viewpoint ? viewpoint.lat : 0, gridType.toLongitude(viewpoint));
    }

    private static Vec2 projectPolar(Vec3 v, GridScale scale) {
        double r = Math.sqrt(v.x * v.x + v.y * v.y);
        double theta = polarAngleRadians(v);
        double scaledr = scale.getYValueInv(r);
        double scaledtheta = scale.getXValueInv(Math.toDegrees(theta));
        return new Vec2(scaledtheta, scaledr);
    }

    private static Vec3 unprojectPolar(Vec2 pt) {
        double r = pt.y;
        Vec2 polarBasis = polarBasis(Math.toRadians(pt.x), r);
        double x = polarBasis.x;
        double y = polarBasis.y;
        double z = Math.sqrt(Math.max(0, 1 - x * x - y * y));
        return new Vec3(x, y, z);
    }

    private static Vec2 projectLatitudinal(Vec3 v, GridScale scale) {
        // Positive latitude corresponds to positive Y in the non-ortho map basis.
        double latitude = Math.asin(Math.clamp(v.y, -1., 1.));
        double longitude = Math.atan2(v.x, v.z);
        double scaledphi = scale.getXValueInv(Math.toDegrees(longitude));
        double scaledtheta = scale.getYValueInv(Math.toDegrees(latitude));
        return new Vec2(scaledphi, scaledtheta);
    }

    private static Vec3 unprojectLatitudinal(Vec2 pt) {
        return latitudinalVector(Math.toRadians(pt.x), Math.toRadians(pt.y));
    }

    private static Vec3 latitudinalVector(double longitude, double latitude) {
        return new Vec3(
                Math.cos(latitude) * Math.sin(longitude),
                Math.sin(latitude),
                Math.cos(latitude) * Math.cos(longitude));
    }

    private static double polarAngleRadians(Vec3 v) {
        // Polar angle is defined as 0 at north and increasing anti-clockwise.
        double theta = Math.atan2(-v.x, v.y);
        theta += 2 * Math.PI;
        theta %= 2 * Math.PI;
        return theta;
    }

    private static Vec2 polarBasis(double angleRadians, double radius) {
        // Positive map angles rotate anti-clockwise from north.
        return new Vec2(-radius * Math.sin(angleRadians), radius * Math.cos(angleRadians));
    }
}
