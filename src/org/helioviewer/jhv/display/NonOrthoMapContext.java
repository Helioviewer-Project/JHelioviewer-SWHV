package org.helioviewer.jhv.display;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

final class NonOrthoMapContext extends MapContext {

    private final ProjectionScale scale;
    private final NonOrthoProjection.Kind kind;
    private final Quat rotation;

    NonOrthoMapContext(Camera camera, Viewport vp, GridType gridType, ProjectionScale scale, NonOrthoProjection.Kind kind) {
        super(camera, vp, gridType);
        this.scale = scale;
        this.kind = kind;
        this.rotation = gridType.mapRotation(camera.getViewpoint());
    }

    @Override
    public ProjectionScale scale() {
        return scale;
    }

    @Override
    public Quat rotation() {
        return rotation;
    }

    @Override
    public boolean isOrthographic() {
        return false;
    }

    @Override
    public boolean isHpc() {
        return kind == NonOrthoProjection.Kind.HPC;
    }

    @Override
    public boolean isLatitudinal() {
        return kind == NonOrthoProjection.Kind.LATITUDINAL;
    }

    @Override
    public boolean isPolar() {
        return kind == NonOrthoProjection.Kind.POLAR && scale == ProjectionScale.polar;
    }

    @Override
    public boolean isLogPolar() {
        return kind == NonOrthoProjection.Kind.POLAR && scale == ProjectionScale.logpolar;
    }

    @Override
    public Vec2 projectToScreen(Vec3 v) {
        return NonOrthoProjection.projectToScreen(kind, this, v);
    }

    @Override
    public Vec3 mouseToSurface(int x, int y) {
        return NonOrthoProjection.mouseToSurface(kind, camera, vp, scale, gridType, x, y);
    }

    @Override
    public Vec2 mouseToGrid(int x, int y) {
        return NonOrthoProjection.mouseToGrid(camera, vp, scale, gridType, x, y);
    }

    @Override
    public Vec2 mouseToScreen(int x, int y) {
        return NonOrthoProjection.mouseToScreen(camera, vp, scale, gridType, x, y);
    }

    @Override
    public Vec2 emitMapVertex(Vec3 vertex, Vec2 previous, boolean first, boolean last, double radius, byte[] color, BufVertex vexBuf) {
        return NonOrthoProjection.emitMapVertex(kind, this, vertex, previous, first, last, color, vexBuf);
    }

    @Override
    public void emitMapPoint(Vec3 vertex, double size, double radius, byte[] color, BufVertex vexBuf) {
        NonOrthoProjection.emitMapPoint(kind, this, vertex, size, color, vexBuf);
    }
}
