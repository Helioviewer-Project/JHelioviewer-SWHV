package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.InteractionAnnotate.AnnotationMode;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLHelper;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class AnnotateCircle extends AbstractAnnotateable {

    public AnnotateCircle(JSONObject jo) {
        super(jo);
    }

    private static void drawCircle(Camera camera, Viewport vp, GL2 gl, Vec3 bp, Vec3 ep) {
        double cosf = Vec3.dot(bp, ep);
        double r = Math.sqrt(1 - cosf * cosf);
        // P = center + r cos(A) (bp x ep) + r sin(A) ep

        Vec3 center = Vec3.multiply(bp, cosf);
        center.multiply(radius);

        Vec3 u = Vec3.cross(bp, ep);
        u.normalize();
        Vec3 v = Vec3.cross(bp, u);

        int subdivs = 90;
        Vec2 previous = null;
        Vec3 vx = new Vec3();

        gl.glBegin(GL2.GL_LINE_STRIP);
        for (int i = 0; i <= subdivs; i++) {
            double t = i * 2. * Math.PI / subdivs;
            double cosr = Math.cos(t) * r;
            double sinr = Math.sin(t) * r;
            vx.x = center.x + cosr * u.x + sinr * v.x;
            vx.y = center.y + cosr * u.y + sinr * v.y;
            vx.z = center.z + cosr * u.z + sinr * v.z;

            if (Display.mode == Display.DisplayMode.Orthographic) {
                gl.glVertex3f((float) vx.x, (float) vx.y, (float) vx.z);
            } else {
                vx.y = -vx.y;
                previous = GLHelper.drawVertex(camera, vp, gl, vx, previous);
            }
        }
        gl.glEnd();
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl, boolean active) {
        if ((startPoint == null || endPoint == null) && !beingDragged())
            return;

        gl.glLineWidth(lineWidth);

        if (beingDragged()) {
            gl.glColor3f(dragColor[0], dragColor[1], dragColor[2]);
            drawCircle(camera, vp, gl, dragStartPoint, dragEndPoint);
        } else {
            if (active)
                gl.glColor3f(activeColor[0], activeColor[1], activeColor[2]);
            else
                gl.glColor3f(baseColor[0], baseColor[1], baseColor[2]);
            drawCircle(camera, vp, gl, startPoint, endPoint);
        }
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
