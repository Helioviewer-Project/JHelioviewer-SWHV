package org.helioviewer.jhv.camera.annotate;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLHelper;
import org.json.JSONObject;

public class AnnotateLoop extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 45;

    private String heightStr = null;

    public AnnotateLoop(JSONObject jo) {
        super(jo);
    }

    private void drawCircle(Quat q, Viewport vp, Vec3 bp, Vec3 ep, BufVertex buf, byte[] color) {
        double cosf = Vec3.dot(bp, ep);
        double r = Math.sqrt(1 - cosf * cosf);
        // P = center + r cos(A) (bp x ep) + r sin(A) ep

        double h = (cosf + r) * Math.sqrt(bp.x * bp.x + bp.y * bp.y + bp.z * bp.z) - Sun.Radius;
        heightStr = h < 0.2 * Sun.Radius ? String.format("Hann: %7.2fMm", h * (Sun.RadiusMeter / 1e6)) : String.format("Hann: %7.2fR\u2609", h);

        Vec3 center = Vec3.multiply(bp, cosf);
        Vec3 u = Vec3.cross(bp, ep);
        u.normalize();
        Vec3 v = Vec3.cross(bp, u);

        Vec3 vx = new Vec3();
        Vec2 previous = null;

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            double t = i * Math.PI / SUBDIVISIONS;
            double cosr = Math.cos(t) * r;
            double sinr = Math.sin(t) * r;
            vx.x = center.x + cosr * v.x + sinr * bp.x;
            vx.y = center.y + cosr * v.y + sinr * bp.y;
            vx.z = center.z + cosr * v.z + sinr * bp.z;
            if (Display.mode == Display.ProjectionMode.Orthographic) {
                if (i == 0) {
                    putSphere(vx, buf, Colors.Null);
                }
                putSphere(vx, buf, color);
                if (i == SUBDIVISIONS) {
                    putSphere(vx, buf, Colors.Null);
                }
            } else {
                vx.y = -vx.y;
                if (i == 0) {
                    GLHelper.drawVertex(q, vp, vx, previous, buf, Colors.Null);
                }
                previous = GLHelper.drawVertex(q, vp, vx, previous, buf, color);
                if (i == SUBDIVISIONS) {
                    GLHelper.drawVertex(q, vp, vx, previous, buf, Colors.Null);
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

        drawCircle(q, vp, p0, p1, buf, color);
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

    @Nullable
    @Override
    public Object getData() {
        return heightStr;
    }

}
