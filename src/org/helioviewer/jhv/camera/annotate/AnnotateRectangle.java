package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLHelper;
import org.json.JSONObject;

public class AnnotateRectangle extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 12;

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
            if (Display.mode == Display.ProjectionMode.Orthographic) {
                if (i == 0) { // first
                    putSphere(pc, buf, Colors.Null);
                }
                putSphere(pc, buf, color);
            } else {
                pc.y = -pc.y;
                if (i == 0) {
                    GLHelper.drawVertex(q, vp, pc, previous, buf, Colors.Null);
                }
                previous = GLHelper.drawVertex(q, vp, pc, previous, buf, color);
            }
        }

        point1 = p2;
        point2 = ep;
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolate(i / (double) SUBDIVISIONS, point1, point2);
            if (Display.mode == Display.ProjectionMode.Orthographic) {
                putSphere(pc, buf, color);
            } else {
                pc.y = -pc.y;
                previous = GLHelper.drawVertex(q, vp, pc, previous, buf, color);
            }
        }

        point1 = ep;
        point2 = p4;
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolate(i / (double) SUBDIVISIONS, point1, point2);
            if (Display.mode == Display.ProjectionMode.Orthographic) {
                putSphere(pc, buf, color);
            } else {
                pc.y = -pc.y;
                previous = GLHelper.drawVertex(q, vp, pc, previous, buf, color);
            }
        }

        point1 = p4;
        point2 = bp;
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolate(i / (double) SUBDIVISIONS, point1, point2);
            if (Display.mode == Display.ProjectionMode.Orthographic) {
                putSphere(pc, buf, color);
                if (i == SUBDIVISIONS) { // last
                    putSphere(pc, buf, Colors.Null);
                }
            } else {
                pc.y = -pc.y;
                previous = GLHelper.drawVertex(q, vp, pc, previous, buf, color);
                if (i == SUBDIVISIONS) {
                    GLHelper.drawVertex(q, vp, pc, previous, buf, Colors.Null);
                }
            }
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
    public void mousePressed(Camera camera, int x, int y) {
        Vec3 pt = computePointSun(camera, x, y);
        if (pt != null)
            dragStartPoint = pt;
    }

    @Override
    public void mouseDragged(Camera camera, int x, int y) {
        Vec3 pt = computePointSun(camera, x, y);
        if (pt != null)
            dragEndPoint = pt;
    }

    @Override
    public void mouseReleased() {
        if (beingDragged()) {
            startPoint = dragStartPoint;
            endPoint = dragEndPoint;
        }
        dragStartPoint = null;
        dragEndPoint = null;
    }

    @Override
    public boolean beingDragged() {
        return dragEndPoint != null && dragStartPoint != null;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public String getType() {
        return Interaction.AnnotationMode.Rectangle.toString();
    }

}
