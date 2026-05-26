package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.RenderView;
import org.helioviewer.jhv.math.PolarBasis;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.wcs.WcsProjection;

final class NonOrthoProjection {

    enum Kind {HPC, LATITUDINAL, POLAR}

    private NonOrthoProjection() {}

    static Vec2 project(Kind kind, Position viewpoint, ProjectionScale scale, Quat rotation, Vec3 v) {
        return switch (kind) {
            case HPC -> projectHpc(viewpoint, v, scale);
            case LATITUDINAL -> projectLatitudinal(rotation, scale, v);
            case POLAR -> projectPolar(rotation, scale, v);
        };
    }

    static Vec3 unproject(Kind kind, Position viewpoint, Quat rotation, Vec2 pt) {
        return switch (kind) {
            case HPC -> unprojectHpc(viewpoint, pt.x, pt.y);
            case LATITUDINAL -> unprojectLatitudinal(rotation, pt.x, pt.y);
            case POLAR -> unprojectPolar(rotation, pt.x, pt.y);
        };
    }

    static Vec3 mouseToSurface(Kind kind, Camera camera, RenderView renderView, Viewport vp, ProjectionScale scale, GridType gridType, int x, int y) {
        Position viewpoint = renderView.viewpoint();
        Quat rotation = gridType.mapRotation(viewpoint);
        return unproject(kind, viewpoint, rotation, mouseToGrid(camera, renderView, vp, scale, gridType, x, y));
    }

    // See docs/non-ortho-projection-note.md for the shared Java/GLSL convention.
    private static Vec2 projectLatitudinal(Quat rotation, ProjectionScale scale, Vec3 v) {
        return projectLatitudinalVector(rotation.rotateVector(v), scale);
    }

    private static Vec3 unprojectLatitudinal(Quat rotation, double longitudeDeg, double latitudeDeg) {
        return rotation.rotateInverseVector(unprojectLatitudinalPoint(longitudeDeg, latitudeDeg));
    }

    private static Vec2 projectPolar(Quat rotation, ProjectionScale scale, Vec3 v) {
        return projectPolarVector(rotation.rotateVector(v), scale);
    }

    private static Vec3 unprojectPolar(Quat rotation, double angleDeg, double radius) {
        return rotation.rotateInverseVector(unprojectPolarPoint(angleDeg, radius));
    }

    private static Vec2 projectHpc(Position viewpoint, Vec3 v, ProjectionScale scale) {
        // External solar points arrive in world space; HPC projection is defined in viewpoint space.
        return projectHpcViewpointSpace(toHpcViewpointSpace(viewpoint, v), viewpoint.distance, scale);
    }

    private static Vec3 unprojectHpc(Position viewpoint, double longitudeDeg, double latitudeDeg) {
        return WcsProjection.helioprojectiveToWorld(viewpoint, Math.toRadians(longitudeDeg), Math.toRadians(latitudeDeg));
    }

    static Vec2 projectToScreen(Kind kind, Position viewpoint, ProjectionScale scale, Quat rotation, Viewport vp, Vec3 v) {
        Vec2 pt = project(kind, viewpoint, scale, rotation, v);
        return new Vec2(pt.x * vp.aspect, pt.y);
    }

    static Vec2 emitMapVertex(Kind kind, Position viewpoint, ProjectionScale scale, Quat rotation, Viewport vp, Vec3 vertex, Vec2 previous, boolean first, boolean last, byte[] color, BufVertex vexBuf) {
        if (kind == Kind.HPC)
            return emitHpcVertex(viewpoint, scale, vp, vertex, previous, first, last, color, vexBuf);

        Vec2 current = project(kind, viewpoint, scale, rotation, vertex);
        if (first)
            emitProjectedVertex(vp, current, Colors.Null, vexBuf);
        emitWrappedVertex(vp, previous, current, color, vexBuf);
        if (last)
            emitProjectedVertex(vp, current, Colors.Null, vexBuf);
        return current;
    }

    static void emitMapPoint(Kind kind, Position viewpoint, ProjectionScale scale, Quat rotation, Viewport vp, Vec3 vertex, double size, byte[] color, BufVertex vexBuf) {
        if (kind == Kind.HPC) {
            emitHpcPoint(viewpoint, scale, vp, vertex, size, color, vexBuf);
            return;
        }

        Vec2 pt = project(kind, viewpoint, scale, rotation, vertex);
        vexBuf.putVertex((float) (pt.x * vp.aspect), (float) pt.y, 0, (float) size, color);
    }

    static Vec2 mouseToScreen(Camera camera, RenderView renderView, Viewport vp, ProjectionScale scale, GridType gridType, int x, int y) {
        Vec2 mouseGrid = mouseToRawGrid(camera, renderView, vp, scale, x, y);
        return new Vec2(
                scale.getXValueInv(mouseGrid.x) * vp.aspect,
                scale.getYValueInv(mouseGrid.y));
    }

    static Vec2 mouseToGrid(Camera camera, RenderView renderView, Viewport vp, ProjectionScale scale, GridType gridType, int x, int y) {
        Vec2 mouseGrid = mouseToRawGrid(camera, renderView, vp, scale, x, y);
        return new Vec2(scale.getDisplayXValue(mouseGrid.x, gridType), mouseGrid.y);
    }

    private static Vec2 mouseToRawGrid(Camera camera, RenderView renderView, Viewport vp, ProjectionScale scale, int x, int y) {
        double width = renderView.cameraWidth(vp.zoom);
        return new Vec2(
                scale.getInterpolatedXValue(ViewportProjection.computeUpX(vp, width, camera.getTranslationX(), x) / vp.aspect + 0.5),
                scale.getInterpolatedYValue(ViewportProjection.computeUpY(vp, width, camera.getTranslationY(), y) + 0.5));
    }

    private static Vec3 toHpcViewpointSpace(Position viewpoint, Vec3 v) {
        return viewpoint.toQuat().rotateVector(v);
    }

    private static Vec2 projectHpcViewpointSpace(Vec3 view, double observerDistance, ProjectionScale scale) {
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

    private static Vec2 projectVisibleHpcSurfacePoint(Position viewpoint, Vec3 vertex, ProjectionScale scale) {
        Vec3 view = toHpcViewpointSpace(viewpoint, vertex);
        if (!isVisibleHpcViewpointSpace(view))
            return null;
        return projectHpcViewpointSpace(view, viewpoint.distance, scale);
    }

    private static Vec2 emitHpcVertex(Position viewpoint, ProjectionScale scale, Viewport vp, Vec3 vertex, Vec2 previous, boolean first, boolean last, byte[] color, BufVertex vexBuf) {
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

    private static void emitHpcPoint(Position viewpoint, ProjectionScale scale, Viewport vp, Vec3 vertex, double size, byte[] color, BufVertex vexBuf) {
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

    private static Vec2 projectPolarVector(Vec3 v, ProjectionScale scale) {
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

    private static Vec2 projectLatitudinalVector(Vec3 v, ProjectionScale scale) {
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
