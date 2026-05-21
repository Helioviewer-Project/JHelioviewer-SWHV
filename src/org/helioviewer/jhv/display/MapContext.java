package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

public abstract class MapContext {

    private final Camera camera;
    private final ProjectionMode mode;
    private final GridType gridType;
    private final Position viewpoint;

    protected MapContext(Camera _camera, ProjectionMode _mode, GridType _gridType, Position _viewpoint) {
        camera = _camera;
        mode = _mode;
        gridType = _gridType;
        viewpoint = _viewpoint;
    }

    public Camera camera() {
        return camera;
    }

    public ProjectionMode mode() {
        return mode;
    }

    public GridType gridType() {
        return gridType;
    }

    public Position viewpoint() {
        return viewpoint;
    }

    public boolean isOrthographic() {
        return mode() == ProjectionMode.Orthographic;
    }

    public boolean isHpc() {
        return mode() == ProjectionMode.HPC;
    }

    public boolean isLatitudinal() {
        return mode() == ProjectionMode.Latitudinal;
    }

    public boolean isPolar() {
        return mode() == ProjectionMode.Polar;
    }

    public boolean isLogPolar() {
        return mode() == ProjectionMode.LogPolar;
    }

    public abstract Vec2 projectToScreen(Viewport vp, ProjectionScale scale, Vec3 v);

    public abstract Vec2 emitMapVertex(Viewport vp, ProjectionScale scale, Vec3 vertex, Vec2 previous, boolean first, boolean last, double radius, byte[] color, BufVertex vexBuf);

    public abstract void emitMapPoint(Viewport vp, ProjectionScale scale, Vec3 vertex, double size, double radius, byte[] color, BufVertex vexBuf);
}
