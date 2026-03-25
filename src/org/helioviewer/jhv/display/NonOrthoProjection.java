package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.math.PolarBasis;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

final class NonOrthoProjection {

    enum Kind {HPC, LATITUDINAL, POLAR}

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
            case HPC -> unprojectHpc(viewpoint, pt.x, pt.y);
            case LATITUDINAL -> unprojectLatitudinal(viewpoint, gridType, pt.x, pt.y);
            case POLAR -> unprojectPolar(viewpoint, gridType, pt.x, pt.y);
        };
    }

    static Vec3 unprojectSurfacePoint(Kind kind, GridScale scale, Camera camera, Viewport vp, int x, int y, GridType gridType) {
        return unproject(kind, camera.getViewpoint(), gridType, mouseToGrid(scale, camera, vp, x, y, gridType));
    }

    // See docs/non-ortho-projection-note.md for the shared Java/GLSL convention.
    private static Vec2 projectLatitudinal(Position viewpoint, GridType gridType, Vec3 v, GridScale scale) {
        return projectLatitudinalVector(gridType.mapRotation(viewpoint).rotateVector(v), scale);
    }

    private static Vec3 unprojectLatitudinal(Position viewpoint, GridType gridType, double longitudeDeg, double latitudeDeg) {
        return gridType.mapRotation(viewpoint).rotateInverseVector(unprojectLatitudinalPoint(longitudeDeg, latitudeDeg));
    }

    private static Vec2 projectPolar(Position viewpoint, GridType gridType, Vec3 v, GridScale scale) {
        return projectPolarVector(gridType.mapRotation(viewpoint).rotateVector(v), scale);
    }

    private static Vec3 unprojectPolar(Position viewpoint, GridType gridType, double angleDeg, double radius) {
        return gridType.mapRotation(viewpoint).rotateInverseVector(unprojectPolarPoint(angleDeg, radius));
    }

    private static Vec2 projectHpc(Position viewpoint, Vec3 v, GridScale scale) {
        // External solar points arrive in world space; HPC projection is defined in viewpoint space.
        return projectHpcViewpointSpace(toHpcViewpointSpace(viewpoint, v), viewpoint.distance, scale);
    }

    private static Vec3 unprojectHpc(Position viewpoint, double longitudeDeg, double latitudeDeg) {
        return helioprojectiveToWorld(viewpoint, Math.toRadians(longitudeDeg), Math.toRadians(latitudeDeg));
    }

    static Vec2 projectToScreen(Kind kind, Position viewpoint, GridType gridType, GridScale scale, Viewport vp, Vec3 v) {
        Vec2 pt = project(kind, viewpoint, gridType, v, scale);
        return new Vec2(pt.x * vp.aspect, pt.y);
    }

    static Vec2 emitMapVertex(Kind kind, Position viewpoint, GridType gridType, GridScale scale, Viewport vp, Vec3 vertex, Vec2 previous, boolean first, boolean last, byte[] color, BufVertex vexBuf) {
        if (kind == Kind.HPC)
            return emitHpcVertex(viewpoint, scale, vp, vertex, previous, first, last, color, vexBuf);

        Vec2 current = project(kind, viewpoint, gridType, vertex, scale);
        if (first)
            emitProjectedVertex(vp, current, Colors.Null, vexBuf);
        emitWrappedVertex(vp, previous, current, color, vexBuf);
        if (last)
            emitProjectedVertex(vp, current, Colors.Null, vexBuf);
        return current;
    }

    static void emitMapPoint(Kind kind, Position viewpoint, GridType gridType, GridScale scale, Viewport vp, Vec3 vertex, double size, byte[] color, BufVertex vexBuf) {
        if (kind == Kind.HPC) {
            emitHpcPoint(viewpoint, scale, vp, vertex, size, color, vexBuf);
            return;
        }

        Vec2 pt = project(kind, viewpoint, gridType, vertex, scale);
        vexBuf.putVertex((float) (pt.x * vp.aspect), (float) pt.y, 0, (float) size, color);
    }

    static Vec2 mouseToScreen(GridScale scale, Camera camera, Viewport vp, int x, int y, GridType gridType) {
        Vec2 mouseGrid = mouseToGrid(scale, camera, vp, x, y, gridType);
        return new Vec2(
                scale.getXValueInv(mouseGrid.x) * vp.aspect,
                scale.getYValueInv(mouseGrid.y));
    }

    static Vec2 mouseToGrid(GridScale scale, Camera camera, Viewport vp, int x, int y, GridType gridType) {
        return new Vec2(
                scale.getInterpolatedXDisplayValue(CameraHelper.computeUpX(camera, vp, x) / vp.aspect + 0.5, gridType),
                scale.getInterpolatedYValue(CameraHelper.computeUpY(camera, vp, y) + 0.5));
    }

    static Vec3 helioprojectiveToWorld(Position viewpoint, double longitude, double latitude) {
        Vec3 ray = helioprojectiveRay(longitude, latitude);

        double b = viewpoint.distance * ray.z;
        double c = viewpoint.distance * viewpoint.distance - 1;
        double discriminant = b * b - c;
        if (discriminant < 0)
            return null;

        double root = Math.sqrt(discriminant);
        double t = -b - root;
        if (t <= 0)
            t = -b + root;
        if (t <= 0)
            return null;

        // Return the inverse in world space to match the rotated projectHpc() path above.
        Vec3 view = new Vec3(t * ray.x, t * ray.y, viewpoint.distance + t * ray.z);
        return viewpoint.toQuat().rotateInverseVector(view);
    }

    private static Vec3 helioprojectiveRay(double longitude, double latitude) {
        double x = Math.tan(longitude);
        double y = Math.tan(latitude) / Math.cos(longitude);
        double invLen = 1. / Math.sqrt(x * x + y * y + 1);
        return new Vec3(x * invLen, y * invLen, -invLen); // normalized
    }

    private static Vec3 toHpcViewpointSpace(Position viewpoint, Vec3 v) {
        return viewpoint.toQuat().rotateVector(v);
    }

    private static Vec2 projectHpcViewpointSpace(Vec3 view, double observerDistance, GridScale scale) {
        double zeta = observerDistance - view.z;
        double longitude = Math.atan2(view.x, zeta);
        double latitude = Math.atan2(view.y, Math.sqrt(view.x * view.x + zeta * zeta));
        return new Vec2(
                scale.getXValueInv(Math.toDegrees(longitude)),
                scale.getYValueInv(Math.toDegrees(latitude)));
    }

    private static boolean isVisibleHpcViewpointSpace(Vec3 view) {
        return view.z >= 0;
    }

    private static Vec2 projectVisibleHpcSurfacePoint(Position viewpoint, Vec3 vertex, GridScale scale) {
        Vec3 view = toHpcViewpointSpace(viewpoint, vertex);
        if (!isVisibleHpcViewpointSpace(view))
            return null;
        return projectHpcViewpointSpace(view, viewpoint.distance, scale);
    }

    private static Vec2 emitHpcVertex(Position viewpoint, GridScale scale, Viewport vp, Vec3 vertex, Vec2 previous, boolean first, boolean last, byte[] color, BufVertex vexBuf) {
        // HPC is a visible-hemisphere map, so hidden segments must terminate the strip.
        Vec2 current = projectVisibleHpcSurfacePoint(viewpoint, vertex, scale);
        if (current == null) {
            if (previous != null)
                vexBuf.repeatVertex(Colors.Null);
            return null;
        }
        if (first || previous == null)
            emitProjectedVertex(vp, current, Colors.Null, vexBuf);
        emitProjectedVertex(vp, current, color, vexBuf);
        if (last)
            emitProjectedVertex(vp, current, Colors.Null, vexBuf);
        return current;
    }

    private static void emitHpcPoint(Position viewpoint, GridScale scale, Viewport vp, Vec3 vertex, double size, byte[] color, BufVertex vexBuf) {
        // Skip back-side surface points in HPC instead of projecting them through the map.
        Vec2 pt = projectVisibleHpcSurfacePoint(viewpoint, vertex, scale);
        if (pt == null)
            return;
        vexBuf.putVertex((float) (pt.x * vp.aspect), (float) pt.y, 0, (float) size, color);
    }

    private static void emitWrappedVertex(Viewport vp, Vec2 previous, Vec2 current, byte[] color, BufVertex vexBuf) {
        if (previous != null && Math.abs(previous.x - current.x) > 0.5) {
            emitHorizontalWrap(vp, current, previous, color, vexBuf);
        }
        emitProjectedVertex(vp, current, color, vexBuf);
    }

    private static void emitHorizontalWrap(Viewport vp, Vec2 current, Vec2 previous, byte[] color, BufVertex vexBuf) {
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

    private static void emitProjectedVertex(Viewport vp, Vec2 projected, byte[] color, BufVertex vexBuf) {
        vexBuf.putVertex((float) (projected.x * vp.aspect), (float) projected.y, 0, 1, color);
    }

    private static Vec2 projectPolarVector(Vec3 v, GridScale scale) {
        double r = Math.sqrt(v.x * v.x + v.y * v.y);
        double theta = PolarBasis.angle(v);
        double scaledr = scale.getYValueInv(r);
        double scaledtheta = scale.getXValueInv(Math.toDegrees(theta));
        return new Vec2(scaledtheta, scaledr);
    }

    private static Vec3 unprojectPolarPoint(double angleDeg, double radius) {
        double theta = Math.toRadians(angleDeg);
        double x = PolarBasis.x(radius, theta);
        double y = PolarBasis.y(radius, theta);
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

    private static Vec3 unprojectLatitudinalPoint(double longitudeDeg, double latitudeDeg) {
        double longitude = Math.toRadians(longitudeDeg);
        double latitude = Math.toRadians(latitudeDeg);
        return SphericalCoords.unit(longitude, latitude);
    }
}
