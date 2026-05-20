package org.helioviewer.jhv.display;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

final class OrthoMapContext extends MapContext {

    OrthoMapContext(Camera _camera, Viewport _vp, GridType _gridType) {
        super(_camera, _vp, _gridType);
    }

    @Override
    public Quat rotation() {
        return null;
    }

    @Override
    public ProjectionMode mode() {
        return ProjectionMode.Orthographic;
    }

    @Override
    public Vec2 emitMapVertex(Vec3 vertex, Vec2 previous, boolean first, boolean last, double radius, byte[] color, BufVertex vexBuf) {
        OrthoProjection.emitMapVertex(vertex, first, last, radius, color, vexBuf);
        return null;
    }

    @Override
    public void emitMapPoint(Vec3 vertex, double size, double radius, byte[] color, BufVertex vexBuf) {
        OrthoProjection.emitMapPoint(vertex, size, radius, color, vexBuf);
    }
}
