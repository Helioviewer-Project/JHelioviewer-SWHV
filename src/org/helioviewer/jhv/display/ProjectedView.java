package org.helioviewer.jhv.display;

import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.PolarBasis;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.wcs.ObserverGeometry;

final class ProjectedView extends MapView {

    ProjectedView(Camera _camera, Position _viewpoint, GridType _gridType, MapMode _mode, MapScale[] _scales) {
        super(_camera, _viewpoint, _mode, _gridType, _scales);
    }

    @Override
    public Vec3 mouseToSurface(Viewport vp, int x, int y) {
        return unproject(mouseToMap(vp, x, y));
    }

    @Override
    public Vec2 mouseToScreen(Viewport vp, int x, int y) {
        double width = cameraWidth(vp);
        return new Vec2(
                ViewportMath.computeUpX(vp, width, camera.getTranslationX(), x),
                ViewportMath.computeUpY(vp, width, camera.getTranslationY(), y));
    }

    private Vec2 projectObserverPoint(MapScale scale, Vec3 view) {
        return switch (mode) {
            case HPC -> projectHpcViewpointSpace(view, viewpoint.distance, scale);
            case RadialWarp -> projectRadialWarp(scale, view);
            case RectWarp -> projectRectWarp(scale, view);
            case Orthographic, Latitudinal -> throw new IllegalArgumentException("Mode does not use observer projection");
        };
    }

    private Vec3 unproject(Vec2 pt) {
        return switch (mode) {
            case HPC -> unprojectHpc(pt.x, pt.y);
            case Latitudinal -> unprojectLatitudinal(pt.x, pt.y);
            case RadialWarp, RectWarp -> unprojectRadialWarp(pt.x, pt.y);
            case Orthographic -> throw new IllegalArgumentException("Orthographic mode is not projected");
        };
    }

    // See docs/non-ortho-projection-note.md for the shared Java/GLSL convention.
    private Vec2 projectLatitudinal(MapScale scale, Vec3 v) {
        double longitude = MathUtils.mapToMinus180To180(
                Math.toDegrees(SphericalCoords.longitude(v) + latiLongitudeOrigin()));
        double latitude = Math.toDegrees(SphericalCoords.latitude(v) - latiLatitudeOrigin());
        return new Vec2(scale.toUnitX(longitude) - 0.5, scale.toUnitY(latitude) - 0.5);
    }

    private Vec3 unprojectLatitudinal(double longitudeDeg, double latitudeDeg) {
        double longitude = Math.toRadians(longitudeDeg) - latiLongitudeOrigin();
        double latitude = Math.toRadians(latitudeDeg) + latiLatitudeOrigin();
        return latitude < -Math.PI / 2 || latitude > Math.PI / 2
                ? null
                : SphericalCoords.unit(longitude, latitude);
    }

    private Vec3 unprojectRadialWarp(double angleDeg, double radius) {
        double theta = Math.toRadians(angleDeg);
        double x = PolarBasis.x(radius, theta);
        double y = PolarBasis.y(radius, theta);
        return ObserverGeometry.observerRayToWorld(viewpoint, new Vec3(x, y, -viewpoint.distance));
    }

    private Vec2 projectRadialWarp(MapScale scale, Vec3 v0) {
        Vec2 hpcXY = projectToHpcPlane(v0);
        double r = Math.hypot(hpcXY.x, hpcXY.y);
        if (r == 0)
            return new Vec2(0, 0);
        double t = Math.max(0, scale.toUnitY(r));
        double f = .5 * t / r;
        return new Vec2(f * hpcXY.x, f * hpcXY.y);
    }

    private Vec2 projectRectWarp(MapScale scale, Vec3 v0) {
        Vec2 hpcXY = projectToHpcPlane(v0);
        double r = Math.hypot(hpcXY.x, hpcXY.y);
        double theta = PolarBasis.angle(hpcXY.x, hpcXY.y);
        return new Vec2(scale.toUnitX(Math.toDegrees(theta)) - 0.5, scale.toUnitY(r) - 0.5);
    }

    private Vec2 projectToHpcPlane(Vec3 v) {
        double fovScale = viewpoint.distance / (viewpoint.distance - v.z);
        return new Vec2(fovScale * v.x, fovScale * v.y);
    }

    private Vec3 unprojectHpc(double longitudeDeg, double latitudeDeg) {
        return ObserverGeometry.helioprojectiveToWorld(viewpoint, Math.toRadians(longitudeDeg), Math.toRadians(latitudeDeg));
    }

    @Override
    public Vec2 projectToScreen(Viewport vp, Vec3 v) {
        MapScale scale = scale(vp);
        Vec2 pt = mode == MapMode.Latitudinal ? projectLatitudinal(scale, v)
                : projectObserverPoint(scale, toHpcViewpointSpace(v));
        return mode == MapMode.RadialWarp ? pt : new Vec2(pt.x * vp.aspect, pt.y);
    }

    @Override
    public void emitMapLine(Viewport vp, List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf) {
        MapScale scale = scale(vp);
        double aspect = mode == MapMode.RadialWarp ? 1 : vp.aspect;
        Vec2 previous = null;
        for (Vec3 vertex : vertices) {
            Vec2 current = projectVisiblePoint(vertex, scale);
            if (current == null) {
                if (previous != null)
                    vexBuf.endLine();
                previous = null;
                continue;
            }
            if (previous == null)
                startProjectedLine(aspect, current, color, vexBuf);
            else if (mode == MapMode.Latitudinal || mode == MapMode.RectWarp)
                emitWrappedVertex(aspect, previous, current, color, vexBuf);
            else
                emitProjectedVertex(aspect, current, color, vexBuf);
            previous = current;
        }
        if (previous != null)
            vexBuf.endLine();
    }

    @Override
    public void emitMapPoints(Viewport vp, List<Vec3> vertices, double size, double radius, byte[] color, BufVertex vexBuf) {
        MapScale scale = scale(vp);
        double aspect = mode == MapMode.RadialWarp ? 1 : vp.aspect;
        float pointSize = (float) size;
        for (Vec3 vertex : vertices) {
            Vec2 pt = projectVisiblePoint(vertex, scale);
            if (pt != null)
                vexBuf.putVertex((float) (pt.x * aspect), (float) pt.y, 0, pointSize, color);
        }
    }

    @Override
    public Vec2 mouseToMap(Viewport vp, int x, int y) {
        double width = cameraWidth(vp);
        MapScale scale = scale(vp);
        if (mode == MapMode.RadialWarp)
            return mouseToRadialWarpMap(width, vp, scale, x, y);
        return new Vec2(
                scale.toMapX(ViewportMath.computeUpX(vp, width, camera.getTranslationX(), x) / vp.aspect + 0.5),
                scale.toMapY(ViewportMath.computeUpY(vp, width, camera.getTranslationY(), y) + 0.5));
    }

    private Vec2 mouseToRadialWarpMap(double width, Viewport vp, MapScale scale, int x, int y) {
        double upX = ViewportMath.computeUpX(vp, width, camera.getTranslationX(), x);
        double upY = ViewportMath.computeUpY(vp, width, camera.getTranslationY(), y);
        double t = 2 * Math.hypot(upX, upY);
        return new Vec2(Math.toDegrees(PolarBasis.angle(upX, upY)), scale.toMapY(t));
    }

    private Vec3 toHpcViewpointSpace(Vec3 v) {
        return viewpoint.toQuat().rotateVector(v);
    }

    private static Vec2 projectHpcViewpointSpace(Vec3 view, double observerDistance, MapScale scale) {
        double zeta = observerDistance - view.z;
        double longitude = Math.atan2(view.x, zeta);
        double latitude = Math.atan2(view.y, Math.sqrt(view.x * view.x + zeta * zeta));
        return new Vec2(
                scale.toUnitX(Math.toDegrees(longitude)) - 0.5,
                scale.toUnitY(Math.toDegrees(latitude)) - 0.5);
    }

    private Vec2 projectVisiblePoint(Vec3 vertex, MapScale scale) {
        if (mode == MapMode.Latitudinal)
            return projectLatitudinal(scale, vertex);
        Vec3 view = toHpcViewpointSpace(vertex);
        double distance = viewpoint.distance;
        double zeta = distance - view.z;
        // Surface points face the observer when D*z >= 1. Below that tangent plane,
        // elevated loop points remain visible if their observer ray clears the sphere.
        // Ray clearance is (D*D - 1)*(x*x + y*y) >= (D - z)*(D - z).
        if (distance * view.z < 1 && (distance - 1) * (distance + 1) * (view.x * view.x + view.y * view.y) < zeta * zeta)
            return null;
        return projectObserverPoint(scale, view);
    }

    private static void emitWrappedVertex(double aspect, Vec2 previous, Vec2 current, byte[] color, BufVertex vexBuf) {
        if (Math.abs(previous.x - current.x) > 0.5)
            emitHorizontalWrap(aspect, current, previous, color, vexBuf);
        emitProjectedVertex(aspect, current, color, vexBuf);
    }

    private static void emitHorizontalWrap(double aspect, Vec2 current, Vec2 previous, byte[] color, BufVertex vexBuf) {
        double edge;
        if (current.x <= 0 && previous.x >= 0)
            edge = 0.5;
        else if (current.x >= 0 && previous.x <= 0)
            edge = -0.5;
        else
            return;

        double dx = current.x + 2 * edge - previous.x;
        // Opposite seam endpoints have the same unwrapped X coordinate.
        float y = (float) (dx == 0 ? current.y : previous.y + (edge - previous.x) / dx * (current.y - previous.y));
        float x = (float) (edge * aspect);
        vexBuf.putVertex(x, y, 0, 1, color);
        vexBuf.endLine();
        vexBuf.startLine(-x, y, 0, 1, color);
    }

    private static void startProjectedLine(double aspect, Vec2 projected, byte[] color, BufVertex vexBuf) {
        vexBuf.startLine((float) (projected.x * aspect), (float) projected.y, 0, 1, color);
    }

    private static void emitProjectedVertex(double aspect, Vec2 projected, byte[] color, BufVertex vexBuf) {
        vexBuf.putVertex((float) (projected.x * aspect), (float) projected.y, 0, 1, color);
    }

}
