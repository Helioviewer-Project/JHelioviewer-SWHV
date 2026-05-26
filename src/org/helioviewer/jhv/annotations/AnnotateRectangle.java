package org.helioviewer.jhv.annotations;

import java.util.List;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.SphericalPoint;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

import org.json.JSONObject;

final class AnnotateRectangle extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 24;

    private final List<Vec3> vertices = fixedSizeVertices(4 * (SUBDIVISIONS + 1));

    AnnotateRectangle(JSONObject jo) {
        super(jo);
    }

    private void drawRectangle(MapView mv, Viewport vp, MapScale scale, SphericalPoint start, SphericalPoint end, byte[] color, BufVertex vexBuf) {
        double startLongitude = start.longitude();
        double startLatitude = start.latitude();
        double endLongitude = end.longitude();
        double endLatitude = end.latitude();
        if (startLongitude * endLongitude < 0) {
            if (endLongitude < startLongitude && startLongitude > Math.PI / 2)
                endLongitude += 2 * Math.PI;
            else if (endLongitude > startLongitude && startLongitude < -Math.PI / 2)
                startLongitude += 2 * Math.PI;
        }

        int vertexIndex = 0;
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolateSpherical(i / (double) SUBDIVISIONS, startLongitude, startLatitude, endLongitude, startLatitude);
            vertices.set(vertexIndex++, pc);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolateSpherical(i / (double) SUBDIVISIONS, endLongitude, startLatitude, endLongitude, endLatitude);
            vertices.set(vertexIndex++, pc);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolateSpherical(i / (double) SUBDIVISIONS, endLongitude, endLatitude, startLongitude, endLatitude);
            vertices.set(vertexIndex++, pc);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolateSpherical(i / (double) SUBDIVISIONS, startLongitude, endLatitude, startLongitude, startLatitude);
            vertices.set(vertexIndex++, pc);
        }
        mv.emitMapLine(vp, scale, vertices, ANNOTATION_RADIUS, color, vexBuf);
    }

    @Override
    public void draw(MapView mv, Viewport vp, MapScale scale, boolean active, BufVertex vexBuf) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        byte[] color = dragged ? dragColor : (active ? activeColor : baseColor);
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;

        SphericalPoint spherical0 = SphericalPoint.fromCartesian(p0);
        SphericalPoint spherical1 = SphericalPoint.fromCartesian(p1);
        drawRectangle(mv, vp, scale, spherical0, spherical1, color, vexBuf);
    }

    @Override
    protected Vec3 computeDragPoint(Camera camera, Viewport vp, int x, int y) {
        return mouseToSurface(camera, vp, x, y);
    }

}
