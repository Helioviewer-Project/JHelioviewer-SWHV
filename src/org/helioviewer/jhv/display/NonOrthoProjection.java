package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.math.PolarBasis;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

final class NonOrthoProjection {

    enum Kind {
        HPC,
        LATITUDINAL,
        POLAR
    }

    private NonOrthoProjection() {
    }

    static Vec2 project(Kind kind, Position viewpoint, GridType gridType, Vec3 v, GridScale scale) {
        return switch (kind) {
            case HPC -> projectHpc(viewpoint, v, scale);
            case LATITUDINAL -> projectLatitudinal(viewpoint, gridType, v, scale);
            case POLAR -> projectPolar(viewpoint, gridType, v, scale);
        };
    }

    static Vec3 unproject(Kind kind, Position viewpoint, GridType gridType, Vec2 pt) {
        return switch (kind) {
            case HPC -> unprojectHpc(viewpoint, pt);
            case LATITUDINAL -> unprojectLatitudinal(viewpoint, gridType, pt);
            case POLAR -> unprojectPolar(viewpoint, gridType, pt);
        };
    }

    // See docs/non-ortho-projection-note.md for the shared Java/GLSL convention.
    static Vec2 projectLatitudinal(Position viewpoint, GridType gridType, Vec3 v, GridScale scale) {
        return projectLatitudinalVector(mapRotation(gridType, viewpoint).rotateVector(v), scale);
    }

    static Vec3 unprojectLatitudinal(Position viewpoint, GridType gridType, Vec2 pt) {
        return mapRotation(gridType, viewpoint).rotateInverseVector(unprojectLatitudinalPoint(pt));
    }

    static Vec2 projectPolar(Position viewpoint, GridType gridType, Vec3 v, GridScale scale) {
        return projectPolarVector(mapRotation(gridType, viewpoint).rotateVector(v), scale);
    }

    static Vec3 unprojectPolar(Position viewpoint, GridType gridType, Vec2 pt) {
        return mapRotation(gridType, viewpoint).rotateInverseVector(unprojectPolarPoint(pt));
    }

    static Vec2 projectHpc(Position viewpoint, Vec3 v, GridScale scale) {
        double zeta = viewpoint.distance - v.z;
        double longitude = Math.atan2(v.x, zeta);
        double latitude = Math.atan2(v.y, Math.sqrt(v.x * v.x + zeta * zeta));
        return new Vec2(
                scale.getXValueInv(Math.toDegrees(longitude)),
                scale.getYValueInv(Math.toDegrees(latitude)));
    }

    static Vec3 unprojectHpc(Position viewpoint, Vec2 pt) {
        Vec3 ray = helioprojectiveRayDegrees(pt);

        double b = viewpoint.distance * ray.z;
        double c = viewpoint.distance * viewpoint.distance - 1;
        double discriminant = b * b - c;
        if (discriminant < 0)
            return null;

        double root = Math.sqrt(discriminant);
        double tNear = -b - root;
        double tFar = -b + root;
        double t = tNear > 0 ? tNear : tFar;
        if (t <= 0)
            return null;

        return new Vec3(t * ray.x, t * ray.y, viewpoint.distance + t * ray.z);
    }

    static Vec2 emitMapVertex(Kind kind, Position viewpoint, GridType gridType, GridScale scale, Viewport vp, Vec3 vertex, Vec2 previous, BufVertex vexBuf, byte[] color, boolean first, boolean last) {
        Vec2 current = project(kind, viewpoint, gridType, vertex, scale);
        if (first)
            emitProjectedVertex(vp, current, vexBuf, Colors.Null);
        if (kind == Kind.HPC)
            emitProjectedVertex(vp, current, vexBuf, color);
        else
            emitWrappedVertex(vp, previous, current, vexBuf, color);
        if (last)
            emitProjectedVertex(vp, current, vexBuf, Colors.Null);
        return current;
    }

    static Vec2 projectToScreen(Kind kind, Position viewpoint, GridType gridType, GridScale scale, Viewport vp, Vec3 v) {
        Vec2 projected = project(kind, viewpoint, gridType, v, scale);
        return new Vec2(projected.x * vp.aspect, projected.y);
    }

    static void emitMapPoint(Kind kind, Position viewpoint, GridType gridType, GridScale scale, Viewport vp, Vec3 vertex, BufVertex vexBuf, byte[] color, double size) {
        Vec2 projected = projectToScreen(kind, viewpoint, gridType, scale, vp, vertex);
        vexBuf.putVertex((float) projected.x, (float) projected.y, 0, (float) size, color);
    }

    static Vec3 unprojectDisplayPoint(Kind kind, Camera camera, Viewport vp, int x, int y) {
        return switch (kind) {
            case HPC -> new Vec3(CameraHelper.computeUpX(camera, vp, x), CameraHelper.computeUpY(camera, vp, y), 0);
            case LATITUDINAL, POLAR -> CameraHelper.unprojectToCurrentViewSphereOrPlane(camera, vp, x, y);
        };
    }

    static Vec2 mouseToScreen(Camera camera, Viewport vp, int x, int y) {
        double gx = CameraHelper.computeUpX(camera, vp, x) / vp.aspect;
        double gy = CameraHelper.computeUpY(camera, vp, y);
        return new Vec2(gx, gy);
    }

    static Vec2 mouseToGrid(Kind kind, GridScale scale, Camera camera, Viewport vp, int x, int y, GridType gridType) {
        Vec2 pt = mouseToScreen(camera, vp, x, y);
        return new Vec2(
                scale.getInterpolatedXDisplayValue(pt.x + 0.5, gridType),
                scale.getInterpolatedYValue(pt.y + 0.5));
    }

    private static Vec3 helioprojectiveRayDegrees(Vec2 pt) {
        double longitude = Math.toRadians(pt.x);
        double latitude = Math.toRadians(pt.y);
        Vec3 ray = new Vec3(
                Math.tan(longitude),
                Math.tan(latitude) / Math.cos(longitude),
                -1);
        ray.normalize();
        return ray;
    }

    private static void emitWrappedVertex(Viewport vp, Vec2 previous, Vec2 current, BufVertex vexBuf, byte[] color) {
        if (previous != null && Math.abs(previous.x - current.x) > 0.5) {
            emitHorizontalWrap(vp, current, previous, vexBuf, color);
        }
        emitProjectedVertex(vp, current, vexBuf, color);
    }

    private static void emitHorizontalWrap(Viewport vp, Vec2 current, Vec2 previous, BufVertex vexBuf, byte[] color) {
        float y = (float) current.y;
        float x;
        if (current.x <= 0 && previous.x >= 0) {
            x = (float) (0.5 * vp.aspect);
            vexBuf.putVertex(x, y, 0, 1, color);
            vexBuf.putVertex(x, y, 0, 1, Colors.Null);

            vexBuf.putVertex(-x, y, 0, 1, Colors.Null);
            vexBuf.putVertex(-x, y, 0, 1, color);
        } else if (current.x >= 0 && previous.x <= 0) {
            x = (float) (-0.5 * vp.aspect);
            vexBuf.putVertex(x, y, 0, 1, color);
            vexBuf.putVertex(x, y, 0, 1, Colors.Null);

            vexBuf.putVertex(-x, y, 0, 1, Colors.Null);
            vexBuf.putVertex(-x, y, 0, 1, color);
        }
    }

    private static void emitProjectedVertex(Viewport vp, Vec2 projected, BufVertex vexBuf, byte[] color) {
        vexBuf.putVertex((float) (projected.x * vp.aspect), (float) projected.y, 0, 1, color);
    }

    private static Quat mapRotation(GridType gridType, Position viewpoint) {
        // Non-ortho maps use GridType.toGrid() longitude, but Viewpoint latitude is
        // rotated with a positive sign to match the shared non-ortho map basis.
        return Quat.createXY(gridType == GridType.Viewpoint ? viewpoint.lat : 0, gridType.toLongitude(viewpoint));
    }

    private static Vec2 projectPolarVector(Vec3 v, GridScale scale) {
        double r = Math.sqrt(v.x * v.x + v.y * v.y);
        double theta = polarAngleRadians(v);
        double scaledr = scale.getYValueInv(r);
        double scaledtheta = scale.getXValueInv(Math.toDegrees(theta));
        return new Vec2(scaledtheta, scaledr);
    }

    private static Vec3 unprojectPolarPoint(Vec2 pt) {
        double r = pt.y;
        double theta = Math.toRadians(pt.x);
        double x = PolarBasis.x(r, theta);
        double y = PolarBasis.y(r, theta);
        double z = Math.sqrt(Math.max(0, 1 - x * x - y * y));
        return new Vec3(x, y, z);
    }

    private static Vec2 projectLatitudinalVector(Vec3 v, GridScale scale) {
        // Positive latitude corresponds to positive Y in the non-ortho map basis.
        double latitude = SphericalCoords.latitude(v);
        double longitude = SphericalCoords.longitude(v);
        double scaledphi = scale.getXValueInv(Math.toDegrees(longitude));
        double scaledtheta = scale.getYValueInv(Math.toDegrees(latitude));
        return new Vec2(scaledphi, scaledtheta);
    }

    private static Vec3 unprojectLatitudinalPoint(Vec2 pt) {
        double longitude = Math.toRadians(pt.x);
        double latitude = Math.toRadians(pt.y);
        return new Vec3(
                SphericalCoords.x(1, longitude, latitude),
                SphericalCoords.y(1, longitude, latitude),
                SphericalCoords.z(1, longitude, latitude));
    }

    private static double polarAngleRadians(Vec3 v) {
        // Polar angle is defined as 0 at north and increasing anti-clockwise.
        double theta = Math.atan2(-v.x, v.y);
        theta += 2 * Math.PI;
        theta %= 2 * Math.PI;
        return theta;
    }
}
