package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

public abstract class MapContext {

    protected final Camera camera;
    protected final Viewport vp;
    protected final GridType gridType;

    protected MapContext(Camera _camera, Viewport _vp, GridType _gridType) {
        camera = _camera;
        vp = _vp;
        gridType = _gridType;
    }

    public Camera camera() {
        return camera;
    }

    public Viewport vp() {
        return vp;
    }

    public GridType gridType() {
        return gridType;
    }

    public Position viewpoint() {
        return camera.getViewpoint();
    }

    public abstract Quat rotation();

    public abstract ProjectionScale scale();

    public abstract boolean isOrthographic();

    public abstract boolean isHpc();

    public abstract boolean isLatitudinal();

    public abstract boolean isPolar();

    public abstract boolean isLogPolar();

    public abstract Vec2 projectToScreen(Vec3 v);

    public abstract Vec3 mouseToSurface(int x, int y);

    public abstract Vec2 mouseToGrid(int x, int y);

    public abstract Vec2 mouseToScreen(int x, int y);

    public abstract Vec2 emitMapVertex(Vec3 vertex, Vec2 previous, boolean first, boolean last, double radius, byte[] color, BufVertex vexBuf);

    public abstract void emitMapPoint(Vec3 vertex, double size, double radius, byte[] color, BufVertex vexBuf);
}
