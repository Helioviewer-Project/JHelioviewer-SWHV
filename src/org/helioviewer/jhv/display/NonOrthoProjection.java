package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;

final class NonOrthoProjection {

    private NonOrthoProjection() {
    }

    static Vec2 projectLatitudinal(Position viewpoint, GridType gridType, Vec3 v, GridScale scale) {
        return projectLatitudinal(rotateToMapBasis(viewpoint, gridType, v), scale);
    }

    static Vec3 unprojectLatitudinal(Position viewpoint, GridType gridType, Vec2 pt) {
        return mapRotation(gridType, viewpoint).rotateInverseVector(unprojectLatitudinal(pt));
    }

    static Vec2 projectPolar(Position viewpoint, GridType gridType, Vec3 v, GridScale scale) {
        return projectPolar(rotateToMapBasis(viewpoint, gridType, v), scale);
    }

    static Vec3 unprojectPolar(Position viewpoint, GridType gridType, Vec2 pt) {
        return mapRotation(gridType, viewpoint).rotateInverseVector(unprojectPolar(pt));
    }

    private static Vec3 rotateToMapBasis(Position viewpoint, GridType gridType, Vec3 v) {
        return mapRotation(gridType, viewpoint).rotateVector(v);
    }

    private static Quat mapRotation(GridType gridType, Position viewpoint) {
        // Non-ortho maps use the same longitude as GridType.toGrid(), but the reflected
        // flat-map basis makes the effective viewpoint latitude rotation positive.
        return Quat.createXY(gridType == GridType.Viewpoint ? viewpoint.lat : 0, gridType.toLongitude(viewpoint));
    }

    private static Vec2 projectPolar(Vec3 v, GridScale scale) {
        double r = Math.sqrt(v.x * v.x + v.y * v.y);
        double theta = Math.atan2(-v.x, v.y);
        theta += 2 * Math.PI;
        theta %= 2 * Math.PI;
        double scaledr = scale.getYValueInv(r);
        double scaledtheta = scale.getXValueInv(Math.toDegrees(theta));
        return new Vec2(scaledtheta, scaledr);
    }

    private static Vec3 unprojectPolar(Vec2 pt) {
        double r = pt.y;
        double theta = -Math.toRadians(pt.x);
        double y = r * Math.cos(theta);
        double x = r * Math.sin(theta);
        double z = Math.sqrt(Math.max(0, 1 - x * x - y * y));
        return new Vec3(x, y, z);
    }

    private static Vec2 projectLatitudinal(Vec3 v, GridScale scale) {
        double theta = Math.asin(Math.clamp(v.y, -1., 1.));
        double phi = Math.atan2(v.x, v.z);
        double scaledphi = scale.getXValueInv(Math.toDegrees(phi));
        double scaledtheta = scale.getYValueInv(Math.toDegrees(theta));
        return new Vec2(scaledphi, scaledtheta);
    }

    private static Vec3 unprojectLatitudinal(Vec2 pt) {
        double phi = Math.toRadians(pt.x);
        double theta = Math.toRadians(pt.y);
        return new Vec3(Math.cos(theta) * Math.sin(phi), Math.sin(theta), Math.cos(theta) * Math.cos(phi));
    }
}
