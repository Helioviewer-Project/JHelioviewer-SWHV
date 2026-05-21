package org.helioviewer.jhv.display;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.RenderView;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

final class NonOrthoMapContext extends MapContext {

    private final NonOrthoProjection.Kind kind;
    private final Quat rotation;

    NonOrthoMapContext(Camera _camera, RenderView _renderView, GridType _gridType, ProjectionMode _mode) {
        super(_camera, _renderView, _mode, _gridType);
        kind = _mode.nonOrthoKind;
        rotation = _gridType.mapRotation(viewpoint());
    }

    @Override
    public Vec2 projectToScreen(Viewport vp, ProjectionScale scale, Vec3 v) {
        return NonOrthoProjection.projectToScreen(kind, viewpoint(), scale, rotation, vp, v);
    }

    @Override
    public Vec2 emitMapVertex(Viewport vp, ProjectionScale scale, Vec3 vertex, Vec2 previous, boolean first, boolean last, double radius, byte[] color, BufVertex vexBuf) {
        return NonOrthoProjection.emitMapVertex(kind, viewpoint(), scale, rotation, vp, vertex, previous, first, last, color, vexBuf);
    }

    @Override
    public void emitMapPoint(Viewport vp, ProjectionScale scale, Vec3 vertex, double size, double radius, byte[] color, BufVertex vexBuf) {
        NonOrthoProjection.emitMapPoint(kind, viewpoint(), scale, rotation, vp, vertex, size, color, vexBuf);
    }
}
