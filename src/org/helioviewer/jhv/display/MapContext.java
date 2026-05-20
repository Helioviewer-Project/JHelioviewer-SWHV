package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

public abstract class MapContext {

    protected final Camera camera;
    protected final GridType gridType;
    protected final Position viewpoint;

    protected MapContext(Camera _camera, GridType _gridType) {
        camera = _camera;
        gridType = _gridType;
        viewpoint = _camera.getViewpoint();
    }

    public Camera camera() {
        return camera;
    }

    public abstract ProjectionMode mode();

    public GridType gridType() {
        return gridType;
    }

    public Position viewpoint() {
        return viewpoint;
    }

    public abstract Quat rotation();

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
