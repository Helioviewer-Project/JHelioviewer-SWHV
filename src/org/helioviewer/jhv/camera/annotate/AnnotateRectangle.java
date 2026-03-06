package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.json.JSONObject;

public class AnnotateRectangle extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 24;

    public AnnotateRectangle(JSONObject jo) {
        super(jo);
    }

    private static void drawRectangle(Quat q, Viewport vp, Vec3 bp, Vec3 ep, BufVertex buf, byte[] color) {
        if (bp.z * ep.z < 0) {
            if (ep.z < bp.z && bp.z > Math.PI / 2)
                ep.z += 2 * Math.PI;
            else if (ep.z > bp.z && bp.z < -Math.PI / 2)
                bp.z += 2 * Math.PI;
        }

        Vec3 p2 = new Vec3(1, ep.y, bp.z);
        Vec3 p4 = new Vec3(1, bp.y, ep.z);
        Vec3 point1, point2;
        Vec2 previous = null;

        point1 = bp;
        point2 = p2;
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolate(i / (double) SUBDIVISIONS, point1, point2);
            previous = Display.mode.drawProjectedVertex(q, vp, pc, previous, buf, color, i == 0, false, radius);
        }

        point1 = p2;
        point2 = ep;
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolate(i / (double) SUBDIVISIONS, point1, point2);
            previous = Display.mode.drawProjectedVertex(q, vp, pc, previous, buf, color, false, false, radius);
        }

        point1 = ep;
        point2 = p4;
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolate(i / (double) SUBDIVISIONS, point1, point2);
            previous = Display.mode.drawProjectedVertex(q, vp, pc, previous, buf, color, false, false, radius);
        }

        point1 = p4;
        point2 = bp;
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolate(i / (double) SUBDIVISIONS, point1, point2);
            previous = Display.mode.drawProjectedVertex(q, vp, pc, previous, buf, color, false, i == SUBDIVISIONS, radius);
        }
    }

    @Override
    public void draw(Quat q, Viewport vp, boolean active, BufVertex buf) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        byte[] color = dragged ? dragColor : (active ? activeColor : baseColor);
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;

        drawRectangle(q, vp, toSpherical(p0), toSpherical(p1), buf, color);
    }

    @Override
    protected Vec3 computeDragPoint(Camera camera, int x, int y) {
        return computePointSun(camera, x, y);
    }

}
