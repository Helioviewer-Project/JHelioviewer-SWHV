package org.helioviewer.jhv.annotation;

import java.util.List;

import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.SphericalPoint;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

import org.json.JSONObject;

final class AnnotateRectangle extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 24;

    private final List<Vec3> vertices = fixedSizeVertices(4 * SUBDIVISIONS + 1);
    private Vec3 geometryStart;
    private Vec3 geometryEnd;

    AnnotateRectangle(JSONObject jo) {
        super(jo);
    }

    private void updateGeometry(Vec3 p0, Vec3 p1) {
        SphericalPoint start = SphericalPoint.fromCartesian(p0);
        SphericalPoint end = SphericalPoint.fromCartesian(p1);
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

        for (int i = 1; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolateSpherical(i / (double) SUBDIVISIONS, endLongitude, startLatitude, endLongitude, endLatitude);
            vertices.set(vertexIndex++, pc);
        }

        for (int i = 1; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolateSpherical(i / (double) SUBDIVISIONS, endLongitude, endLatitude, startLongitude, endLatitude);
            vertices.set(vertexIndex++, pc);
        }

        for (int i = 1; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolateSpherical(i / (double) SUBDIVISIONS, startLongitude, endLatitude, startLongitude, startLatitude);
            vertices.set(vertexIndex++, pc);
        }
    }

    @Override
    public void draw(MapView mv, Viewport vp, BufVertex vexBuf) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        byte[] color = color(dragged);
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;

        if (p0 != geometryStart || p1 != geometryEnd) {
            updateGeometry(p0, p1);
            geometryStart = p0;
            geometryEnd = p1;
        }
        mv.emitMapLine(vp, vertices, ANNOTATION_RADIUS, color, vexBuf);
    }

    @Override
    protected Vec3 computeDragPoint(Viewport vp, int x, int y) {
        return mouseToSurface(vp, x, y);
    }

}
