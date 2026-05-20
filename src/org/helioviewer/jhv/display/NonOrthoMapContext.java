package org.helioviewer.jhv.display;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

final class NonOrthoMapContext extends MapContext {

    private final ProjectionMode mode;
    private final ProjectionScale scale;
    private final NonOrthoProjection.Kind kind;
    private final Quat rotation;

    NonOrthoMapContext(Camera _camera, Viewport _vp, GridType _gridType, ProjectionMode _mode) {
        super(_camera, _vp, _gridType);
        mode = _mode;
        scale = _mode.scale;
        kind = _mode.nonOrthoKind;
        rotation = _gridType.mapRotation(_camera.getViewpoint());
    }

    @Override
    public ProjectionMode mode() {
        return mode;
    }

    @Override
    public Quat rotation() {
        return rotation;
    }

    @Override
    public Vec2 projectToScreen(Vec3 v) {
        return NonOrthoProjection.projectToScreen(kind, viewpoint(), scale, rotation, vp, v);
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
        return NonOrthoProjection.emitMapVertex(kind, viewpoint(), scale, rotation, vp, vertex, previous, first, last, color, vexBuf);
    }

    @Override
    public void emitMapPoint(Vec3 vertex, double size, double radius, byte[] color, BufVertex vexBuf) {
        NonOrthoProjection.emitMapPoint(kind, viewpoint(), scale, rotation, vp, vertex, size, color, vexBuf);
    }
}
