package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.display.Displayer;

import com.jogamp.opengl.GL2;

public class GL3DAnnotateInteraction extends GL3DDefaultInteraction {

    private GL3DVec3d startPoint;
    private GL3DVec3d endPoint;

    private static final double epsilon = 0.01;
    private final ArrayList<GL3DVec3d> points = new ArrayList<GL3DVec3d>();
    private final ArrayList<GL3DVec3d> rectangleStartPoints = new ArrayList<GL3DVec3d>();
    private final ArrayList<GL3DVec3d> rectangleEndPoints = new ArrayList<GL3DVec3d>();
    private int activeIndex = -1;

    protected GL3DAnnotateInteraction(GL3DCamera camera) {
        super(camera);
    }

    private boolean beingDragged() {
        return endPoint != null && startPoint != null;
    }

    private void drawRectangle(GL2 gl, GL3DVec3d bp, GL3DVec3d ep) {
        gl.glBegin(GL2.GL_LINE_LOOP);
        GL3DVec3d p1 = bp;
        GL3DVec3d p2 = new GL3DVec3d(1, ep.y, bp.z);
        GL3DVec3d p3 = ep;
        GL3DVec3d p4 = new GL3DVec3d(1, bp.y, ep.z);

        interpolatedDraw(gl, p1, p2);
        interpolatedDraw(gl, p2, p3);
        interpolatedDraw(gl, p3, p4);
        interpolatedDraw(gl, p4, p1);
        gl.glEnd();
    }

    @Override
    public void drawInteractionFeedback(GL2 gl) {
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

        gl.glBegin(GL2.GL_LINE_LOOP);
        for (int i = 0; i < points.size() - 1; i++) {
            interpolatedDraw(gl, points.get(i), points.get(i + 1));
        }
        gl.glEnd();

        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    private GL3DVec3d toSpherical(GL3DVec3d _p) {
        GL3DVec3d p = camera.getLocalRotation().rotateVector(_p);

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

        return camera.getLocalRotation().rotateInverseVector(pt);
    }

    private void interpolatedDraw(GL2 gl, GL3DVec3d p1s, GL3DVec3d p2s) {
        int subdivisions = 5;

        for (double i = 0; i <= subdivisions; i++) {
            double t = i / subdivisions;
            double y = (1 - t) * p1s.y + t * p2s.y;
            double z = (1 - t) * p1s.z + t * p2s.z;
            GL3DVec3d pc = toCart(1., y, z);
            gl.glVertex3f((float) pc.x, (float) pc.y, (float) pc.z);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        GL3DVec3d pt = camera.getVectorFromSphere(e.getPoint());
        if (pt != null) {
            startPoint = pt;
            Displayer.display();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        GL3DVec3d pt = camera.getVectorFromSphere(e.getPoint());
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
        if (e.getKeyCode() == KeyEvent.VK_R) {
            if (activeIndex >= 0) {
                rectangleEndPoints.remove(activeIndex);
                rectangleStartPoints.remove(activeIndex);
            }
            activeIndex = rectangleEndPoints.size() - 1;
            Displayer.display();
        }

        if (e.getKeyCode() == KeyEvent.VK_N) {
            activeIndex++;
            if (activeIndex >= rectangleEndPoints.size()) {
                activeIndex = 0;
            }
            Displayer.display();
        }
    }

    @Override
    public void reset() {
        rectangleStartPoints.clear();
        rectangleEndPoints.clear();
        super.reset();
    }

}
