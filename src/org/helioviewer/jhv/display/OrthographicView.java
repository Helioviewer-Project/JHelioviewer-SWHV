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
    public Vec2 emitMapVertex(Viewport vp, MapScale scale, Vec3 vertex, Vec2 previous, boolean first, boolean last, double radius, byte[] color, BufVertex vexBuf) {
        OrthographicMap.emitMapVertex(vertex, first, last, radius, color, vexBuf);
        return null;
    }

    @Override
    public void emitMapLine(Viewport vp, MapScale scale, List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf) {
        int last = vertices.size() - 1;
        for (int i = 0; i <= last; i++)
            OrthographicMap.emitMapVertex(vertices.get(i), i == 0, i == last, radius, color, vexBuf);
    }

    @Override
    public void emitMapPoints(Viewport vp, MapScale scale, List<Vec3> vertices, double size, double radius, byte[] color, BufVertex vexBuf) {
        vertices.forEach(vertex -> OrthographicMap.emitMapPoint(vertex, size, radius, color, vexBuf));
    }

    @Override
    public void emitMapPoint(Viewport vp, MapScale scale, Vec3 vertex, double size, double radius, byte[] color, BufVertex vexBuf) {
        OrthographicMap.emitMapPoint(vertex, size, radius, color, vexBuf);
    }
}
