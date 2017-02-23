package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.GLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.jogamp.opengl.GL2;

public class AnnotateCircle extends AbstractAnnotateable {

    @Nullable
    private Vec3 circleStartPoint;
    @Nullable
    private Vec3 circleEndPoint;

    @Nullable
    private Vec3 startPoint;
    @Nullable
    private Vec3 endPoint;

    public AnnotateCircle(Camera _camera) {
        super(_camera);
    }

    private void drawCircle(@NotNull Viewport vp, @NotNull GL2 gl, @NotNull Vec3 bp, @NotNull Vec3 ep) {
        double cosf = bp.dot(ep);
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

            if (Displayer.mode == Displayer.DisplayMode.ORTHO) {
                gl.glVertex3f((float) vx.x, (float) vx.y, (float) vx.z);
            } else {
                vx.y = -vx.y;
                previous = GLHelper.drawVertex(camera, vp, gl, vx, previous);
            }
        }
        gl.glEnd();
    }

    @Override
    public boolean beingDragged() {
        return endPoint != null && startPoint != null;
    }

    @Override
    public void render(@NotNull Viewport vp, @NotNull GL2 gl, boolean active) {
        if ((circleStartPoint == null || circleEndPoint == null) && !beingDragged())
            return;

        gl.glLineWidth(lineWidth);

        if (beingDragged()) {
            gl.glColor3f(dragColor[0], dragColor[1], dragColor[2]);
            drawCircle(vp, gl, startPoint, endPoint);
        } else {
            if (active)
                gl.glColor3f(activeColor[0], activeColor[1], activeColor[2]);
            else
                gl.glColor3f(baseColor[0], baseColor[1], baseColor[2]);
            drawCircle(vp, gl, circleStartPoint, circleEndPoint);
        }
    }

    @Override
    public void mousePressed(int x, int y) {
        Vec3 pt = computePoint(x, y);
        if (pt != null)
            startPoint = pt;
    }

    @Override
    public void mouseDragged(int x, int y) {
        Vec3 pt = computePoint(x, y);
        if (pt != null)
            endPoint = pt;
    }

    @Override
    public void mouseReleased() {
        if (beingDragged()) {
            circleStartPoint = startPoint;
            circleEndPoint = endPoint;
        }
        endPoint = null;
        startPoint = null;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

}
