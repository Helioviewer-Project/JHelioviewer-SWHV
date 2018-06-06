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

public class AnnotateCross extends AbstractAnnotateable {

    private static final double thickness = 0.002;

    public AnnotateCross(JSONObject jo) {
        super(jo);
    }

    private static void drawCross(Camera camera, Viewport vp, GL2 gl, Vec3 bp) {
        double delta = 2.5 * Math.PI / 180;
        Vec3 p1 = new Vec3(radius, bp.y + delta, bp.z);
        Vec3 p2 = new Vec3(radius, bp.y - delta, bp.z);
        Vec3 p3 = new Vec3(radius, bp.y, bp.z + delta);
        Vec3 p4 = new Vec3(radius, bp.y, bp.z - delta);
        gl.glDisable(GL2.GL_DEPTH_TEST);
        interpolatedDraw(camera, vp, gl, p1, p2, 2);
        interpolatedDraw(camera, vp, gl, p3, p4, 2);
        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    private static void interpolatedDraw(Camera camera, Viewport vp, GL2 gl, Vec3 p1s, Vec3 p2s, int subdivisions) {
        if (Display.mode == Display.DisplayMode.Orthographic) {
            gl.glBegin(GL2.GL_TRIANGLE_STRIP);

            for (double i = 0; i < subdivisions; i++) {
                double t = i / subdivisions;
                double y0 = (1 - t) * p1s.y + t * p2s.y;
                double z0 = (1 - t) * p1s.z + t * p2s.z;
                Vec3 p0 = toCart(y0, z0);

                t = (i + 1) / subdivisions;
                double y1 = (1 - t) * p1s.y + t * p2s.y;
                double z1 = (1 - t) * p1s.z + t * p2s.z;
                Vec3 p1 = toCart(y1, z1);

                Vec3 p1minusp0 = Vec3.subtract(p1, p0);
                Vec3 v = Vec3.cross(p0, p1minusp0);
                v.normalize();

                v.multiply(thickness);
                Vec3 p0plusv = Vec3.add(p0, v);
                p0plusv.normalize();
                gl.glVertex3f((float) p0plusv.x, (float) p0plusv.y, (float) p0plusv.z);
                Vec3 p0minusv = Vec3.subtract(p0, v);
                p0minusv.normalize();
                gl.glVertex3f((float) p0minusv.x, (float) p0minusv.y, (float) p0minusv.z);
                if (i == subdivisions - 1) {
                    Vec3 p1plusv = Vec3.add(p1, v);
                    p1plusv.normalize();
                    gl.glVertex3f((float) p1plusv.x, (float) p1plusv.y, (float) p1plusv.z);
                    Vec3 p1minusv = Vec3.subtract(p1, v);
                    p1minusv.normalize();
                    gl.glVertex3f((float) p1minusv.x, (float) p1minusv.y, (float) p1minusv.z);
                }
            }

            gl.glEnd();
        } else {
            gl.glBegin(GL2.GL_LINE_STRIP);

            Vec2 previous = null;
            for (double i = 0; i <= subdivisions; i++) {
                double t = i / subdivisions;
                double y0 = (1 - t) * p1s.y + t * p2s.y;
                double z0 = (1 - t) * p1s.z + t * p2s.z;
                Vec3 p0 = toCart(y0, z0);
                p0.y = -p0.y;
                previous = GLHelper.drawVertex(camera, vp, gl, p0, previous);
            }

            gl.glEnd();
        }
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl, boolean active) {
        if (startPoint == null)
            return;

        gl.glLineWidth(lineWidth);

        if (active)
            gl.glColor3f(activeColor[0], activeColor[1], activeColor[2]);
        else
            gl.glColor3f(baseColor[0], baseColor[1], baseColor[2]);
        drawCross(camera, vp, gl, toSpherical(startPoint));
    }

    @Override
    public void mousePressed(Camera camera, int x, int y) {
        Vec3 pt = computePoint(camera, x, y);
        if (pt != null)
            startPoint = pt;
    }

    @Override
    public void mouseDragged(Camera camera, int x, int y) {
    }

    @Override
    public void mouseReleased() {
    }

    @Override
    public boolean beingDragged() {
        return true;
    }

    @Override
    public boolean isDraggable() {
        return false;
    }

    @Override
    public String getType() {
        return AnnotationMode.Cross.toString();
    }

}
