package org.helioviewer.jhv.display;

import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

public abstract class MapView {

    static final double NORMALIZED_FIT_WIDTH = 1.1;

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
        return mode == MapMode.Orthographic
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

    public boolean isRadialWarp() {
        return mode == MapMode.RadialWarp;
    }

    public boolean isRectWarp() {
        return mode == MapMode.RectWarp;
    }

    public Vec3 mouseToSky(Viewport vp, int x, int y) {
        return ViewportMath.unprojectToCurrentViewSphereOrPlane(camera, vp, cameraWidth(vp), x, y);
    }

    public Vec3 mouseToPlane(Viewport vp, int x, int y) { // only for SWEKPopupController
        return ViewportMath.unprojectToOutputPlane(camera, vp, cameraWidth(vp), x, y, Quat.ZERO);
    }

    public abstract Vec2 projectToScreen(Viewport vp, Vec3 v);

    public abstract Vec2 mouseToMap(Viewport vp, int x, int y);

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
        public Vec2 mouseToMap(Viewport vp, int x, int y) {
            return OrthographicMap.mouseToMap(camera, viewpoint, cameraWidth(vp), vp, gridType, x, y);
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

        private final Quat rotation;

        ProjectedView(Camera _camera, Position _viewpoint, GridType _gridType, MapMode _mode, MapScale[] _scales) {
            super(_camera, _viewpoint, _mode, _gridType, _scales);
            rotation = _gridType.mapRotation(viewpoint);
        }

        @Override
        public double cameraWidth(Viewport vp) {
            return mode.usesNormalizedFitWidth() ? mode.normalizedFitWidth() * vp.zoom : super.cameraWidth(vp);
        }

        @Override
        public Vec2 projectToScreen(Viewport vp, Vec3 v) {
            return ProjectedMap.projectToScreen(mode, viewpoint, scale(vp), rotation, vp, v);
        }

        @Override
        public Vec2 mouseToMap(Viewport vp, int x, int y) {
            return ProjectedMap.mouseToMap(mode, camera, cameraWidth(vp), vp, scale(vp), x, y);
        }

        @Override
        public Vec3 mouseToSurface(Viewport vp, int x, int y) {
            return ProjectedMap.unproject(mode, viewpoint, rotation, mouseToMap(vp, x, y));
        }

        @Override
        public Vec2 mouseToScreen(Viewport vp, int x, int y) {
            double width = cameraWidth(vp);
            return new Vec2(
                    ViewportMath.computeUpX(vp, width, camera.getTranslationX(), x),
                    ViewportMath.computeUpY(vp, width, camera.getTranslationY(), y));
        }

        @Override
        public void emitMapLine(Viewport vp, List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf) {
            ProjectedMap.emitMapLine(mode, viewpoint, scale(vp), rotation, vp, vertices, color, vexBuf);
        }

        @Override
        public void emitMapPoints(Viewport vp, List<Vec3> vertices, double size, double radius, byte[] color, BufVertex vexBuf) {
            ProjectedMap.emitMapPoints(mode, viewpoint, scale(vp), rotation, vp, vertices, size, color, vexBuf);
        }
    }
}
