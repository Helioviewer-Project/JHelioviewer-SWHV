package org.helioviewer.jhv.camera.annotate;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.opengl.GLHelper;

import com.jogamp.opengl.GL2;

public class AnnotateRectangle extends AbstractAnnotateable {

    private final ArrayList<Vec3> rectangleStartPoints = new ArrayList<Vec3>();
    private final ArrayList<Vec3> rectangleEndPoints = new ArrayList<Vec3>();

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

    private boolean beingDragged() {
        return endPoint != null && startPoint != null;
    }

    private void interpolatedDraw(GL2 gl, Vec3 p1s, Vec3 p2s) {
        double delta = Math.PI * 2.5 / 180;
        int subdivisions = (int) Math.max(Math.abs(p1s.y - p2s.y) / delta, Math.abs(p1s.z - p2s.z) / delta);

        for (double i = 0; i <= subdivisions; i++) {
            double t = i / subdivisions;
            double y = (1 - t) * p1s.y + t * p2s.y;
            double z = (1 - t) * p1s.z + t * p2s.z;
            Vec3 pc = toCart(camera, radius, y, z);
            gl.glVertex3f((float) pc.x, (float) pc.y, (float) pc.z);
        }
    }

    @Override
    public void render(GL2 gl) {
        if (rectangleStartPoints.size() == 0 && !beingDragged())
            return;

        GLHelper.lineWidth(gl, lineWidth);

        gl.glColor3f(dragColor[0], dragColor[1], dragColor[2]);
        if (beingDragged()) {
            drawRectangle(gl, toSpherical(camera, startPoint), toSpherical(camera, endPoint));
        }

        gl.glColor3f(baseColor[0], baseColor[1], baseColor[2]);
        int sz = rectangleStartPoints.size();
        for (int i = 0; i < sz; i++) {
            if (i != activeIndex)
                drawRectangle(gl, toSpherical(camera, rectangleStartPoints.get(i)), toSpherical(camera, rectangleEndPoints.get(i)));
        }

        gl.glColor3f(activeColor[0], activeColor[1], activeColor[2]);
        if (sz - 1 >= 0)
            drawRectangle(gl, toSpherical(camera, rectangleStartPoints.get(activeIndex)), toSpherical(camera, rectangleEndPoints.get(activeIndex)));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Vec3 pt = camera.getVectorFromSphere(e.getPoint());
        if (pt != null) {
            endPoint = pt;
            Displayer.display();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (beingDragged()) {
            rectangleStartPoints.add(startPoint);
            rectangleEndPoints.add(endPoint);
            activeIndex = rectangleEndPoints.size() - 1;
        }

        endPoint = null;
        startPoint = null;
        Displayer.display();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE) {
            if (activeIndex >= 0) {
                rectangleEndPoints.remove(activeIndex);
                rectangleStartPoints.remove(activeIndex);
            }
            activeIndex = rectangleEndPoints.size() - 1;
            Displayer.display();
        } else if (code == KeyEvent.VK_N) {
            if (activeIndex >= 0) {
                activeIndex++;
                activeIndex = activeIndex % rectangleStartPoints.size();
                Displayer.display();
            }
        }
    }

    @Override
    public void reset() {
        rectangleStartPoints.clear();
        rectangleEndPoints.clear();
        activeIndex = -1;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Vec3 pt = camera.getVectorFromSphere(e.getPoint());
        if (pt != null) {
            startPoint = pt;
        }
    }

}
