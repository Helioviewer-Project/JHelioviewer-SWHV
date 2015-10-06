package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.display.Displayer;

import com.jogamp.opengl.GL2;

public class GL3DAnnotateRectangle {
    private final ArrayList<GL3DVec3d> rectangleStartPoints = new ArrayList<GL3DVec3d>();
    private final ArrayList<GL3DVec3d> rectangleEndPoints = new ArrayList<GL3DVec3d>();
    private int activeIndex = -1;
    private static final double radius = 1.01;
    private GL3DVec3d startPoint;
    private GL3DVec3d endPoint;

    private void drawRectangle(GL2 gl, GL3DVec3d bp, GL3DVec3d ep) {
        gl.glBegin(GL2.GL_LINE_STRIP);

        if (bp.z * ep.z < 0) {
            if (ep.z < bp.z && bp.z > Math.PI / 2)
                ep.z += 2 * Math.PI;
            else if (ep.z > bp.z && bp.z < -Math.PI / 2)
                bp.z += 2 * Math.PI;
        }

        GL3DVec3d p1 = bp;
        GL3DVec3d p2 = new GL3DVec3d(radius, ep.y, bp.z);
        GL3DVec3d p3 = ep;
        GL3DVec3d p4 = new GL3DVec3d(radius, bp.y, ep.z);

        interpolatedDraw(gl, p1, p2);
        interpolatedDraw(gl, p2, p3);
        interpolatedDraw(gl, p3, p4);
        interpolatedDraw(gl, p4, p1);
        gl.glEnd();
    }

    private boolean beingDragged() {
        return endPoint != null && startPoint != null;
    }

    private void interpolatedDraw(GL2 gl, GL3DVec3d p1s, GL3DVec3d p2s) {
        int subdivisions = 5;

        for (double i = 0; i <= subdivisions; i++) {
            double t = i / subdivisions;
            double y = (1 - t) * p1s.y + t * p2s.y;
            double z = (1 - t) * p1s.z + t * p2s.z;
            GL3DVec3d pc = toCart(radius, y, z);
            gl.glVertex3f((float) pc.x, (float) pc.y, (float) pc.z);
        }
    }

    public void render(GL2 gl) {
        if (rectangleStartPoints.size() == 0 && !beingDragged())
            return;

        gl.glDisable(GL2.GL_TEXTURE_2D);

        gl.glLineWidth(2.0f);

        gl.glColor3f(1f, 1f, 0f);
        if (beingDragged()) {
            drawRectangle(gl, toSpherical(startPoint), toSpherical(endPoint));
        }

        gl.glColor3f(0f, 0f, 1f);
        int sz = rectangleStartPoints.size();
        for (int i = 0; i < sz; i++) {
            if (i != activeIndex)
                drawRectangle(gl, toSpherical(rectangleStartPoints.get(i)), toSpherical(rectangleEndPoints.get(i)));
        }

        gl.glColor3f(1f, 0f, 0f);
        if (sz - 1 >= 0)
            drawRectangle(gl, toSpherical(rectangleStartPoints.get(activeIndex)), toSpherical(rectangleEndPoints.get(activeIndex)));

        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    private GL3DVec3d toSpherical(GL3DVec3d _p) {
        GL3DVec3d p = Displayer.getViewport().getCamera().getLocalRotation().rotateVector(_p);

        GL3DVec3d pt = new GL3DVec3d();
        pt.x = p.length();
        pt.y = Math.acos(p.y / pt.x);
        pt.z = Math.atan2(p.x, p.z);

        return pt;
    }

    private GL3DVec3d toCart(double x, double y, double z) {
        GL3DVec3d pt = new GL3DVec3d();
        pt.z = x * Math.sin(y) * Math.cos(z);
        pt.x = x * Math.sin(y) * Math.sin(z);
        pt.y = x * Math.cos(y);

        return Displayer.getViewport().getCamera().getLocalRotation().rotateInverseVector(pt);
    }

    public void mouseDragged(MouseEvent e) {
        GL3DVec3d pt = Displayer.getViewport().getCamera().getVectorFromSphere(e.getPoint());
        if (pt != null) {
            endPoint = pt;
            Displayer.display();
        }
    }

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
            activeIndex++;
            if (activeIndex >= rectangleEndPoints.size()) {
                activeIndex = 0;
            }
            Displayer.display();
        }
    }

    public void reset() {
        rectangleStartPoints.clear();
        rectangleEndPoints.clear();
    }

    public void mousePressed(MouseEvent e) {
        GL3DVec3d pt = Displayer.getViewport().getCamera().getVectorFromSphere(e.getPoint());
        if (pt != null) {
            startPoint = pt;
        }
    }
}