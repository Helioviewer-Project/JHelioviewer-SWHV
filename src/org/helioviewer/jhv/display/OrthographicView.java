package org.helioviewer.jhv.display;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.RenderView;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

final class OrthographicView extends MapView {

    OrthographicView(Camera _camera, RenderView _renderView, GridType _gridType) {
        super(_camera, _renderView, MapMode.Orthographic, _gridType);
    }

    @Override
    public Vec2 projectToScreen(Viewport vp, MapScale scale, Vec3 v) {
        throw new UnsupportedOperationException("Orthographic mode does not support projectToScreen()");
    }

    @Override
    public Vec2 emitMapVertex(Viewport vp, MapScale scale, Vec3 vertex, Vec2 previous, boolean first, boolean last, double radius, byte[] color, BufVertex vexBuf) {
        OrthographicMap.emitMapVertex(vertex, first, last, radius, color, vexBuf);
        return null;
    }

    @Override
    public void emitMapPoint(Viewport vp, MapScale scale, Vec3 vertex, double size, double radius, byte[] color, BufVertex vexBuf) {
        OrthographicMap.emitMapPoint(vertex, size, radius, color, vexBuf);
    }
}
