package org.helioviewer.jhv.display;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.RenderView;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

final class ProjectedView extends MapView {

    private final ProjectedMap.Kind kind;
    private final Quat rotation;

    ProjectedView(Camera _camera, RenderView _renderView, GridType _gridType, MapMode _mode) {
        super(_camera, _renderView, _mode, _gridType);
        kind = _mode.projectedKind;
        rotation = _gridType.mapRotation(viewpoint());
    }

    @Override
    public Vec2 projectToScreen(Viewport vp, MapScale scale, Vec3 v) {
        return ProjectedMap.projectToScreen(kind, viewpoint(), scale, rotation, vp, v);
    }

    @Override
    public Vec2 emitMapVertex(Viewport vp, MapScale scale, Vec3 vertex, Vec2 previous, boolean first, boolean last, double radius, byte[] color, BufVertex vexBuf) {
        return ProjectedMap.emitMapVertex(kind, viewpoint(), scale, rotation, vp, vertex, previous, first, last, color, vexBuf);
    }

    @Override
    public void emitMapPoint(Viewport vp, MapScale scale, Vec3 vertex, double size, double radius, byte[] color, BufVertex vexBuf) {
        ProjectedMap.emitMapPoint(kind, viewpoint(), scale, rotation, vp, vertex, size, color, vexBuf);
    }
}
