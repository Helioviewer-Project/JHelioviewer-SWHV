package org.helioviewer.jhv.camera.annotate;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;

import com.jogamp.opengl.GL2;

public class AnnotateRectangle extends AbstractAnnotateable {

    private Vec3 rectangleStartPoint;
    private Vec3 rectangleEndPoint;

    private Vec3 startPoint;
    private Vec3 endPoint;

    public AnnotateRectangle(Camera _camera) {
        super(_camera);
    }

    private void drawRectangle(GL2 gl, Vec3 bp, Vec3 ep) {
        gl.glBegin(GL2.GL_LINE_STRIP);

        if (bp.z * ep.z < 0) {
            if (ep.z < bp.z && bp.z > Math.PI / 2)
                ep.z += 2 * Math.PI;
            else if (ep.z > bp.z && bp.z < -Math.PI / 2)
                bp.z += 2 * Math.PI;
        }

        Vec3 p1 = bp;
        Vec3 p2 = new Vec3(radius, ep.y, bp.z);
        Vec3 p3 = ep;
        Vec3 p4 = new Vec3(radius, bp.y, ep.z);

        interpolatedDraw(gl, p1, p2);
        interpolatedDraw(gl, p2, p3);
        interpolatedDraw(gl, p3, p4);
        interpolatedDraw(gl, p4, p1);
        gl.glEnd();
    }

    @Override
    public boolean beingDragged() {
        return endPoint != null && startPoint != null;
    }

    private void interpolatedDraw(GL2 gl, Vec3 p1s, Vec3 p2s) {
        double delta = Math.PI * 2.5 / 180;
        int subdivisions = (int) Math.max(Math.abs(p1s.y - p2s.y) / delta, Math.abs(p1s.z - p2s.z) / delta);

        for (double i = 0; i <= subdivisions; i++) {
            double t = i / subdivisions;
            double y = (1 - t) * p1s.y + t * p2s.y;
            double z = (1 - t) * p1s.z + t * p2s.z;
            Vec3 pc = toCart(radius, y, z);

            if (Displayer.mode != Displayer.DisplayMode.ORTHO) {
                pc.y = -pc.y;
                Vec3 pt = camera.getViewpoint().orientation.rotateVector(pc);
                Vec2 tf = GridScale.current.transform(pt);

                gl.glVertex2f((float) (tf.x * Displayer.getActiveViewport().aspect), (float) tf.y);
            } else {
                gl.glVertex3f((float) pc.x, (float) pc.y, (float) pc.z);
            }
        }
    }

    @Override
    public void render(GL2 gl, boolean active) {
        if ((rectangleStartPoint == null || rectangleEndPoint == null) && !beingDragged())
            return;

        gl.glLineWidth(lineWidth);

        if (beingDragged()) {
            gl.glColor3f(dragColor[0], dragColor[1], dragColor[2]);
            drawRectangle(gl, toSpherical(startPoint), toSpherical(endPoint));
        } else {
            if (active)
                gl.glColor3f(activeColor[0], activeColor[1], activeColor[2]);
            else
                gl.glColor3f(baseColor[0], baseColor[1], baseColor[2]);
            drawRectangle(gl, toSpherical(rectangleStartPoint), toSpherical(rectangleEndPoint));
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Vec3 pt = computePoint(e.getPoint());
        if (pt != null) {
            endPoint = pt;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (beingDragged()) {
            rectangleStartPoint = startPoint;
            rectangleEndPoint = endPoint;
        }
        endPoint = null;
        startPoint = null;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Vec3 pt = computePoint(e.getPoint());
        if (pt != null)
            startPoint = pt;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }
}
