package org.helioviewer.jhv.display;

import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

public abstract class MapView {

    private final Camera camera;
    private final Position viewpoint;
    private final double cameraWidth;
    private final Quat viewRotation;
    private final MapMode mode;
    private final GridType gridType;

    protected MapView(Camera _camera, Position _viewpoint, double _cameraWidth, MapMode _mode, GridType _gridType) {
        camera = _camera;
        viewpoint = _viewpoint;
        cameraWidth = _cameraWidth;
        viewRotation = Quat.rotate(_camera.getDragRotation(), _viewpoint.toQuat());
        mode = _mode;
        gridType = _gridType;
    }

    protected final Camera camera() {
        return camera;
    }

    static MapView orthographic(Camera camera, Position viewpoint, GridType gridType) {
        return new OrthographicView(camera, viewpoint, camera.getCameraWidth(1), gridType);
    }

    static MapView orthographic(Camera camera, Position viewpoint, double width, GridType gridType) {
        return new OrthographicView(camera, viewpoint, width, gridType);
    }

    static MapView projected(Camera camera, Position viewpoint, GridType gridType, MapMode mode) {
        return new ProjectedView(camera, viewpoint, camera.getCameraWidth(1), gridType, mode);
    }

    static MapView projected(Camera camera, Position viewpoint, double width, GridType gridType, MapMode mode) {
        return new ProjectedView(camera, viewpoint, width, gridType, mode);
    }

    public double cameraWidth(Viewport vp) {
        return cameraWidth * vp.zoom;
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

    public boolean isOrthographic() {
        return mode() == MapMode.Orthographic;
    }

    public boolean isHpc() {
        return mode() == MapMode.HPC;
    }

    public boolean isLatitudinal() {
        return mode() == MapMode.Latitudinal;
    }

    public boolean isPolar() {
        return mode() == MapMode.Polar;
    }

    public boolean isLogPolar() {
        return mode() == MapMode.LogPolar;
    }

    public Vec3 mouseToSky(Viewport vp, int x, int y) {
        return ViewportMath.unprojectToCurrentViewSphereOrPlane(camera, vp, cameraWidth(vp), x, y);
    }

    public abstract Vec2 projectToScreen(Viewport vp, MapScale scale, Vec3 v);

    public abstract Vec2 mouseToGrid(Viewport vp, int x, int y);

    public abstract Vec3 mouseToSurface(Viewport vp, int x, int y);

    public abstract Vec2 mouseToScreen(Viewport vp, int x, int y);

    public abstract void emitMapLine(Viewport vp, MapScale scale, List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf);

    public abstract void emitMapPoints(Viewport vp, MapScale scale, List<Vec3> vertices, double size, double radius, byte[] color, BufVertex vexBuf);

    private static final class OrthographicView extends MapView {

        OrthographicView(Camera _camera, Position _viewpoint, double _cameraWidth, GridType _gridType) {
            super(_camera, _viewpoint, _cameraWidth, MapMode.Orthographic, _gridType);
        }

        @Override
        public Vec2 projectToScreen(Viewport vp, MapScale scale, Vec3 v) {
            throw new UnsupportedOperationException("Orthographic mode does not support projectToScreen()");
        }

        @Override
        public Vec2 mouseToGrid(Viewport vp, int x, int y) {
            return OrthographicMap.mouseToGrid(camera(), viewpoint(), cameraWidth(vp), vp, gridType(), x, y);
        }

        @Override
        public Vec3 mouseToSurface(Viewport vp, int x, int y) {
            return OrthographicMap.mouseToSurface(camera(), viewpoint(), cameraWidth(vp), vp, x, y);
        }

        @Override
        public Vec2 mouseToScreen(Viewport vp, int x, int y) {
            throw new UnsupportedOperationException("Orthographic mode does not support mouseToScreen()");
        }

        @Override
        public void emitMapLine(Viewport vp, MapScale scale, List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf) {
            OrthographicMap.emitMapLine(vertices, radius, color, vexBuf);
        }

        @Override
        public void emitMapPoints(Viewport vp, MapScale scale, List<Vec3> vertices, double size, double radius, byte[] color, BufVertex vexBuf) {
            OrthographicMap.emitMapPoints(vertices, size, radius, color, vexBuf);
        }
    }

    private static final class ProjectedView extends MapView {

        private final ProjectedMap.Kind kind;
        private final Quat rotation;

        ProjectedView(Camera _camera, Position _viewpoint, double _cameraWidth, GridType _gridType, MapMode _mode) {
            super(_camera, _viewpoint, _cameraWidth, _mode, _gridType);
            kind = _mode.projectedKind;
            rotation = _gridType.mapRotation(viewpoint());
        }

        @Override
        public Vec2 projectToScreen(Viewport vp, MapScale scale, Vec3 v) {
            return ProjectedMap.projectToScreen(kind, viewpoint(), scale, rotation, vp, v);
        }

        @Override
        public Vec2 mouseToGrid(Viewport vp, int x, int y) {
            return ProjectedMap.mouseToGrid(camera(), cameraWidth(vp), vp, mode().scale, gridType(), x, y);
        }

        @Override
        public Vec3 mouseToSurface(Viewport vp, int x, int y) {
            return ProjectedMap.mouseToSurface(kind, camera(), viewpoint(), cameraWidth(vp), vp, mode().scale, gridType(), x, y);
        }

        @Override
        public Vec2 mouseToScreen(Viewport vp, int x, int y) {
            return ProjectedMap.mouseToScreen(camera(), cameraWidth(vp), vp, mode().scale, x, y);
        }

        @Override
        public void emitMapLine(Viewport vp, MapScale scale, List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf) {
            ProjectedMap.emitMapLine(kind, viewpoint(), scale, rotation, vp, vertices, color, vexBuf);
        }

        @Override
        public void emitMapPoints(Viewport vp, MapScale scale, List<Vec3> vertices, double size, double radius, byte[] color, BufVertex vexBuf) {
            ProjectedMap.emitMapPoints(kind, viewpoint(), scale, rotation, vp, vertices, size, color, vexBuf);
        }
    }
}
