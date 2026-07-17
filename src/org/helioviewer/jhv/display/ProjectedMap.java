package org.helioviewer.jhv.display;

import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.math.PolarBasis;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.wcs.WcsProjection;

final class ProjectedMap {

    private static Vec2 project(MapMode mode, Position viewpoint, MapScale scale, Quat rotation, Vec3 v) {
        return switch (mode) {
            case HPC -> projectHpc(viewpoint, v, scale);
            case Latitudinal -> projectLatitudinal(rotation, scale, v);
            case RadialWarp -> projectRadialWarp(viewpoint, scale, v);
            case RectWarp -> projectRectWarp(viewpoint, scale, v);
            case Orthographic -> throw new IllegalArgumentException("Orthographic mode is not projected");
        };
    }

    static Vec3 unproject(MapMode mode, Position viewpoint, Quat rotation, Vec2 pt) {
        return switch (mode) {
            case HPC -> unprojectHpc(viewpoint, pt.x, pt.y);
            case Latitudinal -> unprojectLatitudinal(rotation, pt.x, pt.y);
            case RadialWarp, RectWarp -> unprojectRadialWarp(viewpoint, pt.x, pt.y);
            case Orthographic -> throw new IllegalArgumentException("Orthographic mode is not projected");
        };
    }

    // See docs/non-ortho-projection-note.md for the shared Java/GLSL convention.
    private static Vec2 projectLatitudinal(Quat rotation, MapScale scale, Vec3 v) {
        return projectLatitudinalVector(rotation.rotateVector(v), scale);
    }

    private static Vec3 unprojectLatitudinal(Quat rotation, double longitudeDeg, double latitudeDeg) {
        return rotation.rotateInverseVector(unprojectLatitudinalPoint(longitudeDeg, latitudeDeg));
    }

    private static Vec3 unprojectRadialWarp(Position viewpoint, double angleDeg, double radius) {
        double theta = Math.toRadians(angleDeg);
        double x = PolarBasis.x(radius, theta);
        double y = PolarBasis.y(radius, theta);
        return WcsProjection.helioprojectiveToWorld(
                viewpoint,
                Math.atan2(x, viewpoint.distance),
                Math.atan2(y, Math.sqrt(x * x + viewpoint.distance * viewpoint.distance)));
    }

    private static Vec2 projectRadialWarp(Position viewpoint, MapScale scale, Vec3 v0) {
        Vec2 hpcXY = projectToHpcPlane(viewpoint, v0);
        double r = Math.hypot(hpcXY.x, hpcXY.y);
        if (r == 0)
            return new Vec2(0, 0);
        double t = Math.max(0, scale.toUnitY(r));
        double f = .5 * t / r;
        return new Vec2(f * hpcXY.x, f * hpcXY.y);
    }

    private static Vec2 projectRectWarp(Position viewpoint, MapScale scale, Vec3 v0) {
        Vec2 hpcXY = projectToHpcPlane(viewpoint, v0);
        double r = Math.hypot(hpcXY.x, hpcXY.y);
        double theta = PolarBasis.angle(hpcXY.x, hpcXY.y);
        return new Vec2(scale.toUnitX(Math.toDegrees(theta)) - 0.5, scale.toUnitY(r) - 0.5);
    }

    private static Vec2 projectToHpcPlane(Position viewpoint, Vec3 v0) {
        Vec3 v = toHpcViewpointSpace(viewpoint, v0);
        double fovScale = viewpoint.distance / (viewpoint.distance - v.z);
        return new Vec2(fovScale * v.x, fovScale * v.y);
    }

    private static Vec2 projectHpc(Position viewpoint, Vec3 v, MapScale scale) {
        // External solar points arrive in world space; HPC projection is defined in viewpoint space.
        return projectHpcViewpointSpace(toHpcViewpointSpace(viewpoint, v), viewpoint.distance, scale);
    }

    private static Vec3 unprojectHpc(Position viewpoint, double longitudeDeg, double latitudeDeg) {
        return WcsProjection.helioprojectiveToWorld(viewpoint, Math.toRadians(longitudeDeg), Math.toRadians(latitudeDeg));
    }

    static Vec2 projectToScreen(MapMode mode, Position viewpoint, MapScale scale, Quat rotation, Viewport vp, Vec3 v) {
        Vec2 pt = project(mode, viewpoint, scale, rotation, v);
        return mode == MapMode.RadialWarp ? pt : new Vec2(pt.x * vp.aspect, pt.y);
    }

    static void emitMapLine(MapMode mode, Position viewpoint, MapScale scale, Quat rotation, Viewport vp, List<Vec3> vertices, byte[] color, BufVertex vexBuf) {
        if (vertices.isEmpty())
            return;
        if (mode == MapMode.HPC) {
            emitHpcLine(viewpoint, scale, vp, vertices, color, vexBuf);
            return;
        }
        if (mode == MapMode.RadialWarp) {
            emitRadialWarpLine(viewpoint, scale, vertices, color, vexBuf);
            return;
        }

        Vec2 current = project(mode, viewpoint, scale, rotation, vertices.getFirst());
        emitProjectedVertex(vp, current, Colors.Null, vexBuf);
        vexBuf.repeatVertex(color);
        for (int i = 1; i < vertices.size(); i++) {
            Vec2 previous = current;
            current = project(mode, viewpoint, scale, rotation, vertices.get(i));
            emitWrappedVertex(vp, previous, current, color, vexBuf);
        }
        vexBuf.repeatVertex(Colors.Null);
    }

    private static void emitRadialWarpLine(Position viewpoint, MapScale scale, List<Vec3> vertices, byte[] color, BufVertex vexBuf) {
        Vec2 current = projectRadialWarp(viewpoint, scale, vertices.getFirst());
        vexBuf.putVertex((float) current.x, (float) current.y, 0, 1, Colors.Null);
        vexBuf.repeatVertex(color);
        for (int i = 1; i < vertices.size(); i++) {
            current = projectRadialWarp(viewpoint, scale, vertices.get(i));
            vexBuf.putVertex((float) current.x, (float) current.y, 0, 1, color);
        }
        vexBuf.repeatVertex(Colors.Null);
    }

    private static void emitHpcLine(Position viewpoint, MapScale scale, Viewport vp, List<Vec3> vertices, byte[] color, BufVertex vexBuf) {
        // HPC is a visible-hemisphere map, so hidden segments must terminate the strip.
        Vec2 previous = null;
        int last = vertices.size() - 1;
        for (int i = 0; i <= last; i++) {
            Vec2 current = projectVisibleHpcSurfacePoint(viewpoint, vertices.get(i), scale);
            if (current == null) {
                if (previous != null)
                    vexBuf.repeatVertex(Colors.Null);
                previous = null;
                continue;
            }

            if (i == 0 || previous == null) {
                emitProjectedVertex(vp, current, Colors.Null, vexBuf);
                vexBuf.repeatVertex(color);
            } else {
                emitProjectedVertex(vp, current, color, vexBuf);
            }
            if (i == last)
                vexBuf.repeatVertex(Colors.Null);
            previous = current;
        }
    }

    static void emitMapPoints(MapMode mode, Position viewpoint, MapScale scale, Quat rotation, Viewport vp, List<Vec3> vertices, double size, byte[] color, BufVertex vexBuf) {
        if (mode == MapMode.HPC) {
            emitHpcPoints(viewpoint, scale, vp, vertices, size, color, vexBuf);
            return;
        }

        float pointSize = (float) size;
        if (mode == MapMode.RadialWarp) {
            for (Vec3 vertex : vertices) {
                Vec2 pt = projectRadialWarp(viewpoint, scale, vertex);
                vexBuf.putVertex((float) pt.x, (float) pt.y, 0, pointSize, color);
            }
            return;
        }
        for (Vec3 vertex : vertices) {
            Vec2 pt = project(mode, viewpoint, scale, rotation, vertex);
            vexBuf.putVertex((float) (pt.x * vp.aspect), (float) pt.y, 0, pointSize, color);
        }
    }

    private static void emitHpcPoints(Position viewpoint, MapScale scale, Viewport vp, List<Vec3> vertices, double size, byte[] color, BufVertex vexBuf) {
        // Skip back-side surface points in HPC instead of projecting them through the map.
        float pointSize = (float) size;
        for (Vec3 vertex : vertices) {
            Vec2 pt = projectVisibleHpcSurfacePoint(viewpoint, vertex, scale);
            if (pt != null)
                vexBuf.putVertex((float) (pt.x * vp.aspect), (float) pt.y, 0, pointSize, color);
        }
    }

    static Vec2 mouseToMap(MapMode mode, Camera camera, double width, Viewport vp, MapScale scale, int x, int y) {
        if (mode == MapMode.RadialWarp)
            return mouseToRadialWarpMap(camera, width, vp, scale, x, y);
        return new Vec2(
                scale.toMapX(ViewportMath.computeUpX(vp, width, camera.getTranslationX(), x) / vp.aspect + 0.5),
                scale.toMapY(ViewportMath.computeUpY(vp, width, camera.getTranslationY(), y) + 0.5));
    }

    private static Vec2 mouseToRadialWarpMap(Camera camera, double width, Viewport vp, MapScale scale, int x, int y) {
        double upX = ViewportMath.computeUpX(vp, width, camera.getTranslationX(), x);
        double upY = ViewportMath.computeUpY(vp, width, camera.getTranslationY(), y);
        double t = 2 * Math.hypot(upX, upY);
        return new Vec2(Math.toDegrees(PolarBasis.angle(upX, upY)), scale.toMapY(t));
    }

    private static Vec3 toHpcViewpointSpace(Position viewpoint, Vec3 v) {
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

    private static boolean isVisibleHpcViewpointSpace(Vec3 view) {
        return view.z >= 0;
    }

    private static Vec2 projectVisibleHpcSurfacePoint(Position viewpoint, Vec3 vertex, MapScale scale) {
        Vec3 view = toHpcViewpointSpace(viewpoint, vertex);
        if (!isVisibleHpcViewpointSpace(view))
            return null;
        return projectHpcViewpointSpace(view, viewpoint.distance, scale);
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
            vexBuf.repeatVertex(Colors.Null);

            vexBuf.putVertex(-x, y, 0, 1, Colors.Null);
            vexBuf.repeatVertex(color);
        } else if (current.x >= 0 && previous.x <= 0) {
            x = (float) (-0.5 * vp.aspect);
            vexBuf.putVertex(x, y, 0, 1, color);
            vexBuf.repeatVertex(Colors.Null);

            vexBuf.putVertex(-x, y, 0, 1, Colors.Null);
            vexBuf.repeatVertex(color);
        }
    }

    private static void emitProjectedVertex(Viewport vp, Vec2 projected, byte[] color, BufVertex vexBuf) {
        vexBuf.putVertex((float) (projected.x * vp.aspect), (float) projected.y, 0, 1, color);
    }

    private static Vec2 projectLatitudinalVector(Vec3 v, MapScale scale) {
        // Positive latitude corresponds to positive Y in the non-ortho map basis.
        double latitude = SphericalCoords.latitude(v);
        double longitude = SphericalCoords.longitude(v);
        double scaledphi = scale.toUnitX(Math.toDegrees(longitude)) - 0.5;
        double scaledtheta = scale.toUnitY(Math.toDegrees(latitude)) - 0.5;
        return new Vec2(scaledphi, scaledtheta);
    }

    private static Vec3 unprojectLatitudinalPoint(double longitudeDeg, double latitudeDeg) {
        double longitude = Math.toRadians(longitudeDeg);
        double latitude = Math.toRadians(latitudeDeg);
        return SphericalCoords.unit(longitude, latitude);
    }

    private ProjectedMap() {}
}
