package org.helioviewer.jhv.display;

import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.RenderView;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

public abstract class MapView {

    private final Camera camera;
    private final RenderView renderView;
    private final MapMode mode;
    private final GridType gridType;

    protected MapView(Camera _camera, RenderView _renderView, MapMode _mode, GridType _gridType) {
        camera = _camera;
        renderView = _renderView;
        mode = _mode;
        gridType = _gridType;
    }

    static MapView orthographic(Camera camera, RenderView renderView, GridType gridType) {
        return new OrthographicView(camera, renderView, gridType);
    }

    static MapView projected(Camera camera, RenderView renderView, GridType gridType, MapMode mode) {
        return new ProjectedView(camera, renderView, gridType, mode);
    }

    public Camera camera() {
        return camera;
    }

    public double cameraWidth(Viewport vp) {
        return renderView.cameraWidth(vp.zoom);
    }

    public MapMode mode() {
        return mode;
    }

    public GridType gridType() {
        return gridType;
    }

    public Position viewpoint() {
        return renderView.viewpoint();
    }

    public Vec2 mouseToGrid(Viewport vp, int x, int y) {
        return mode.mouseToGrid(camera, renderView, vp, gridType, x, y);
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

    public abstract Vec2 projectToScreen(Viewport vp, MapScale scale, Vec3 v);

    public abstract void emitMapLine(Viewport vp, MapScale scale, List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf);

    public abstract void emitMapPoints(Viewport vp, MapScale scale, List<Vec3> vertices, double size, double radius, byte[] color, BufVertex vexBuf);

    private static final class OrthographicView extends MapView {

        OrthographicView(Camera _camera, RenderView _renderView, GridType _gridType) {
            super(_camera, _renderView, MapMode.Orthographic, _gridType);
        }

        @Override
        public Vec2 projectToScreen(Viewport vp, MapScale scale, Vec3 v) {
            throw new UnsupportedOperationException("Orthographic mode does not support projectToScreen()");
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

        ProjectedView(Camera _camera, RenderView _renderView, GridType _gridType, MapMode _mode) {
            super(_camera, _renderView, _mode, _gridType);
            kind = _mode.projectedKind;
            rotation = _gridType.mapRotation(viewpoint());
        }

        @Override
        public Vec2 projectToScreen(Viewport vp, MapScale scale, Vec3 v) {
            return ProjectedMap.projectToScreen(kind, viewpoint(), scale, rotation, vp, v);
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
