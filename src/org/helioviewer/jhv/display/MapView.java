package org.helioviewer.jhv.display;

import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.RenderView;
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

    public abstract Vec2 emitMapVertex(Viewport vp, MapScale scale, Vec3 vertex, Vec2 previous, boolean first, boolean last, double radius, byte[] color, BufVertex vexBuf);

    public abstract void emitMapLine(Viewport vp, MapScale scale, List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf);

    public abstract void emitMapPoint(Viewport vp, MapScale scale, Vec3 vertex, double size, double radius, byte[] color, BufVertex vexBuf);
}
