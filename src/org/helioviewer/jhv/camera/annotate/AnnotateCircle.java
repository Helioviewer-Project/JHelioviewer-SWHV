package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.FloatArray;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.InteractionAnnotate.AnnotationMode;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLSLPolyline;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class AnnotateCircle extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 90;

    private final GLSLPolyline line = new GLSLPolyline();

    public AnnotateCircle(JSONObject jo) {
        super(jo);
    }

    @Override
    public void init(GL2 gl) {
        line.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        line.dispose(gl);
    }

    private static void drawCircle(Camera camera, Viewport vp, Vec3 bp, Vec3 ep, FloatArray pos, FloatArray col, float[] color) {
        double cosf = Vec3.dot(bp, ep);
        double r = Math.sqrt(1 - cosf * cosf);
        // P = center + r cos(A) (bp x ep) + r sin(A) ep

        Vec3 center = Vec3.multiply(bp, cosf);
        center.multiply(radius);

        Vec3 u = Vec3.cross(bp, ep);
        u.normalize();
        Vec3 v = Vec3.cross(bp, u);

        Vec2 previous = null;
        Vec3 vx = new Vec3();

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            double t = i * 2. * Math.PI / SUBDIVISIONS;
            double cosr = Math.cos(t) * r;
            double sinr = Math.sin(t) * r;
            vx.x = center.x + cosr * u.x + sinr * v.x;
            vx.y = center.y + cosr * u.y + sinr * v.y;
            vx.z = center.z + cosr * u.z + sinr * v.z;
            if (Display.mode == Display.DisplayMode.Orthographic) {
                if (i == 0) {
                    pos.put3f((float) vx.x, (float) vx.y, (float) vx.z);
                    col.put4f(BufferUtils.colorNull);
                }
                pos.put3f((float) vx.x, (float) vx.y, (float) vx.z);
                col.put4f(color);
                if (i == SUBDIVISIONS) {
                    pos.put3f((float) vx.x, (float) vx.y, (float) vx.z);
                    col.put4f(BufferUtils.colorNull);
                }
            } else {
                vx.y = -vx.y;
                if (i == 0) {
                    GLHelper.drawVertex(camera, vp, vx, previous, pos, col, BufferUtils.colorNull);
                }
                previous = GLHelper.drawVertex(camera, vp, vx, previous, pos, col, color);
                if (i == SUBDIVISIONS) {
                    GLHelper.drawVertex(camera, vp, vx, previous, pos, col, BufferUtils.colorNull);
                }
            }
        }
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl, boolean active) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        float[] color = dragged ? dragColor : (active ? activeColor : baseColor);
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;

        FloatArray pos = new FloatArray();
        FloatArray col = new FloatArray();

        drawCircle(camera, vp, p0, p1, pos, col, color);
        line.setData(gl, pos.toBuffer(), col.toBuffer());
        line.render(gl, vp, LINEWIDTH);
    }

    @Override
    public void mousePressed(Camera camera, int x, int y) {
        Vec3 pt = computePoint(camera, x, y);
        if (pt != null)
            dragStartPoint = pt;
    }

    @Override
    public void mouseDragged(Camera camera, int x, int y) {
        Vec3 pt = computePoint(camera, x, y);
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
        return AnnotationMode.Circle.toString();
    }

}
