package org.helioviewer.jhv.display;

import java.util.List;

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
    public void emitMapLine(Viewport vp, MapScale scale, List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf) {
        OrthographicMap.emitMapLine(vertices, radius, color, vexBuf);
    }

    @Override
    public void emitMapPoints(Viewport vp, MapScale scale, List<Vec3> vertices, double size, double radius, byte[] color, BufVertex vexBuf) {
        OrthographicMap.emitMapPoints(vertices, size, radius, color, vexBuf);
    }
}
