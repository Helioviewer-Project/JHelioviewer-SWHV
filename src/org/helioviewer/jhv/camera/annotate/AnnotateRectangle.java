package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.SphericalPoint;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.json.JSONObject;

public class AnnotateRectangle extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 24;

    public AnnotateRectangle(JSONObject jo) {
        super(jo);
    }

    private static void drawRectangle(Position viewpoint, GridType gridType, Viewport vp, SphericalPoint start, SphericalPoint end, BufVertex buf, byte[] color) {
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
        Vec2 previous = null;

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolateSpherical(i / (double) SUBDIVISIONS, startLongitude, startLatitude, endLongitude, startLatitude);
            previous = Display.mode.emitMapVertex(viewpoint, gridType, vp, pc, previous, buf, color, i == 0, false, ANNOTATION_RADIUS);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolateSpherical(i / (double) SUBDIVISIONS, endLongitude, startLatitude, endLongitude, endLatitude);
            previous = Display.mode.emitMapVertex(viewpoint, gridType, vp, pc, previous, buf, color, false, false, ANNOTATION_RADIUS);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolateSpherical(i / (double) SUBDIVISIONS, endLongitude, endLatitude, startLongitude, endLatitude);
            previous = Display.mode.emitMapVertex(viewpoint, gridType, vp, pc, previous, buf, color, false, false, ANNOTATION_RADIUS);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolateSpherical(i / (double) SUBDIVISIONS, startLongitude, endLatitude, startLongitude, startLatitude);
            previous = Display.mode.emitMapVertex(viewpoint, gridType, vp, pc, previous, buf, color, false, i == SUBDIVISIONS, ANNOTATION_RADIUS);
        }
    }

    @Override
    public void draw(Position viewpoint, GridType gridType, Viewport vp, boolean active, BufVertex buf) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        byte[] color = dragged ? dragColor : (active ? activeColor : baseColor);
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;

        SphericalPoint spherical0 = SphericalPoint.fromCartesian(p0);
        SphericalPoint spherical1 = SphericalPoint.fromCartesian(p1);
        drawRectangle(viewpoint, gridType, vp, spherical0, spherical1, buf, color);
    }

    @Override
    protected Vec3 computeDragPoint(Camera camera, Viewport vp, int x, int y) {
        return computePointSurface(camera, vp, x, y);
    }

}
