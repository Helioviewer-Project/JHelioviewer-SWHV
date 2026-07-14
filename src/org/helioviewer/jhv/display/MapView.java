package org.helioviewer.jhv.display;

import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

public abstract class MapView {

    // The disk projection works in a normalized radial space (rim at w-radius 0.5), so it fits
    // the viewport at camera width ~1 regardless of the orthographic R_sun FOV. A small margin
    // keeps the outer rim off the very edge.
    static final double DISK_FIT_WIDTH = 1.1;

    protected final Camera camera;
    protected final Position viewpoint;
    protected final MapMode mode;
    protected final GridType gridType;
    private final MapScale[] scales;
    private final Quat dragRotation;
    private final Quat viewRotation;

    private MapView(Camera _camera, Position _viewpoint, MapMode _mode, GridType _gridType, MapScale[] _scales) {
        camera = _camera;
        viewpoint = _viewpoint;
        mode = _mode;
        gridType = _gridType;
        scales = _scales;
        dragRotation = camera.getDragRotation();
        viewRotation = Quat.rotate(dragRotation, viewpoint.toQuat());
    }

    static MapView create(Camera camera, Position viewpoint, GridType gridType, MapMode mode, MapScale[] scales) {
        return mode.kind == MapMode.Kind.ORTHOGRAPHIC
                ? new OrthographicView(camera, viewpoint, gridType, mode, scales)
                : new ProjectedView(camera, viewpoint, gridType, mode, scales);
    }

    public double cameraWidth(Viewport vp) {
        return camera.baseCameraWidth() * vp.zoom;
    }

    public double cameraTranslationX() {
        return camera.getTranslationX();
    }

    public double cameraTranslationY() {
        return camera.getTranslationY();
    }

    public MapMode mode() {
        return mode;
    }

    public GridType gridType() {
        return gridType;
    }

    public Position viewpoint() {
        return viewpoint;
    }

    public Quat viewRotation() {
        return viewRotation;
    }

    public MapScale scale(Viewport vp) {
        return scales[vp.idx];
    }

    public Quat dragRotation() { // only for ViewpointLayer
        return dragRotation;
    }

    public boolean isOrthographic() {
        return mode == MapMode.Orthographic;
    }

    public boolean isHpc() {
        return mode == MapMode.HPC;
    }

    public boolean isLatitudinal() {
        return mode == MapMode.Latitudinal;
    }

    public boolean isPolar() {
        return mode == MapMode.Polar;
    }

    public boolean isLogPolar() {
        return mode == MapMode.LogPolar;
    }

    public boolean isDisk() {
        return mode.kind == MapMode.Kind.DISK;
    }

    public Vec3 mouseToSky(Viewport vp, int x, int y) {
        return ViewportMath.unprojectToCurrentViewSphereOrPlane(camera, vp, cameraWidth(vp), x, y);
    }

    public Vec3 mouseToPlane(Viewport vp, int x, int y) { // only for SWEKPopupController
        return ViewportMath.unprojectToOutputPlane(camera, vp, cameraWidth(vp), x, y, Quat.ZERO);
    }

    public abstract Vec2 projectToScreen(Viewport vp, Vec3 v);

    public abstract Vec2 mouseToGrid(Viewport vp, int x, int y);

    public abstract Vec3 mouseToSurface(Viewport vp, int x, int y);

    public abstract Vec2 mouseToScreen(Viewport vp, int x, int y); // only for SWEKPopupController

    public abstract void emitMapLine(Viewport vp, List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf);

    public abstract void emitMapPoints(Viewport vp, List<Vec3> vertices, double size, double radius, byte[] color, BufVertex vexBuf);

    private static final class OrthographicView extends MapView {

        OrthographicView(Camera _camera, Position _viewpoint, GridType _gridType, MapMode _mode, MapScale[] _scales) {
            super(_camera, _viewpoint, _mode, _gridType, _scales);
        }

        @Override
        public Vec2 projectToScreen(Viewport vp, Vec3 v) {
            throw new UnsupportedOperationException("Orthographic mode does not support projectToScreen()");
        }

        @Override
        public Vec2 mouseToGrid(Viewport vp, int x, int y) {
            return OrthographicMap.mouseToGrid(camera, viewpoint, cameraWidth(vp), vp, gridType, x, y);
        }

        @Override
        public Vec3 mouseToSurface(Viewport vp, int x, int y) {
            return OrthographicMap.mouseToSurface(camera, viewpoint, cameraWidth(vp), vp, x, y);
        }

        @Override
        public Vec2 mouseToScreen(Viewport vp, int x, int y) {
            throw new UnsupportedOperationException("Orthographic mode does not support mouseToScreen()");
        }

        @Override
        public void emitMapLine(Viewport vp, List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf) {
            OrthographicMap.emitMapLine(vertices, radius, color, vexBuf);
        }

        @Override
        public void emitMapPoints(Viewport vp, List<Vec3> vertices, double size, double radius, byte[] color, BufVertex vexBuf) {
            OrthographicMap.emitMapPoints(vertices, size, radius, color, vexBuf);
        }
    }

    private static final class ProjectedView extends MapView {

        private final ProjectedMap.Kind kind;
        private final Quat rotation;

        ProjectedView(Camera _camera, Position _viewpoint, GridType _gridType, MapMode _mode, MapScale[] _scales) {
            super(_camera, _viewpoint, _mode, _gridType, _scales);
            kind = projectedKind(_mode.kind);
            rotation = _gridType.mapRotation(viewpoint);
        }

        private static ProjectedMap.Kind projectedKind(MapMode.Kind kind) {
            return switch (kind) {
                case HPC -> ProjectedMap.Kind.HPC;
                case LATITUDINAL -> ProjectedMap.Kind.LATITUDINAL;
                case POLAR -> ProjectedMap.Kind.POLAR;
                case DISK -> ProjectedMap.Kind.DISK;
                case ORTHOGRAPHIC -> throw new IllegalArgumentException("Orthographic mode has no projected kind");
            };
        }

        // The disk must not inherit Ortho's R_sun camera width (it would collapse to a dot when
        // switched in from a zoomed-out view); fit it to the viewport and let the wheel zoom
        // (vp.zoom) scale it from there. Render and mouse mapping both read this, so they stay in sync.
        @Override
        public double cameraWidth(Viewport vp) {
            return kind == ProjectedMap.Kind.DISK ? DISK_FIT_WIDTH * vp.zoom : super.cameraWidth(vp);
        }

        @Override
        public Vec2 projectToScreen(Viewport vp, Vec3 v) {
            return ProjectedMap.projectToScreen(kind, viewpoint, scale(vp), rotation, vp, v);
        }

        @Override
        public Vec2 mouseToGrid(Viewport vp, int x, int y) {
            return ProjectedMap.mouseToGrid(kind, camera, cameraWidth(vp), vp, scale(vp), gridType, x, y);
        }

        @Override
        public Vec3 mouseToSurface(Viewport vp, int x, int y) {
            return ProjectedMap.mouseToSurface(kind, camera, viewpoint, cameraWidth(vp), vp, scale(vp), gridType, x, y);
        }

        @Override
        public Vec2 mouseToScreen(Viewport vp, int x, int y) {
            return ProjectedMap.mouseToScreen(kind, camera, cameraWidth(vp), vp, scale(vp), x, y);
        }

        @Override
        public void emitMapLine(Viewport vp, List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf) {
            ProjectedMap.emitMapLine(kind, viewpoint, scale(vp), rotation, vp, vertices, color, vexBuf);
        }

        @Override
        public void emitMapPoints(Viewport vp, List<Vec3> vertices, double size, double radius, byte[] color, BufVertex vexBuf) {
            ProjectedMap.emitMapPoints(kind, viewpoint, scale(vp), rotation, vp, vertices, size, color, vexBuf);
        }
    }
}
