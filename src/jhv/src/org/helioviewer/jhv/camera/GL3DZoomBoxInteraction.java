package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.display.Displayer;

import com.jogamp.opengl.GL2;

/**
 * The zoom box interaction allows the user to select a region of interest in
 * the scene by dragging. The camera then moves accordingly so that only the
 * selected region is contained within the view frustum. If the zoom box is
 * restricted to the solar disk, the camera panning will be reset and a rotation
 * is applied. When the zoom box intersects with the corona the rotation is
 * reset and only a panning is applied.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DZoomBoxInteraction extends GL3DDefaultInteraction {

    private GL3DVec3d zoomBoxStartPoint;
    private GL3DVec3d zoomBoxEndPoint;
    private static final double epsilon = 0.01;
    private final ArrayList<GL3DVec3d> points = new ArrayList<GL3DVec3d>();
    private final ArrayList<GL3DVec3d> rectangleStartPoints = new ArrayList<GL3DVec3d>();
    private final ArrayList<GL3DVec3d> rectangleEndPoints = new ArrayList<GL3DVec3d>();
    private int activeIndex = -1;

    public GL3DZoomBoxInteraction(GL3DCamera camera) {
        super(camera);
    }

    private boolean isValidZoomBox() {
        return this.zoomBoxEndPoint != null && this.zoomBoxStartPoint != null;
    }

    public void drawRectangle(GL2 gl, GL3DVec3d bp, GL3DVec3d ep) {
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
    public void drawInteractionFeedback(GL2 gl, GL3DCamera camera) {
        gl.glDisable(GL2.GL_TEXTURE_2D);

        gl.glLineWidth(2.0f);

        gl.glColor3f(1f, 1f, 0f);
        if (this.isValidZoomBox()) {
            drawRectangle(gl, toSpherical(zoomBoxStartPoint), toSpherical(zoomBoxEndPoint));
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

    public GL3DVec3d toSpherical(GL3DVec3d _p) {
        GL3DCamera activeCamera = Displayer.getViewport().getCamera();
        GL3DVec3d p = activeCamera.getLocalRotation().rotateVector(_p);

        GL3DVec3d pt = new GL3DVec3d();
        pt.x = p.length();
        pt.y = Math.acos(p.y / pt.x);
        pt.z = Math.atan2(p.x, p.z);
        return pt;
    }

    public GL3DVec3d toCart(GL3DVec3d p) {
        GL3DVec3d pt = new GL3DVec3d();
        pt.z = p.x * Math.sin(p.y) * Math.cos(p.z);
        pt.x = p.x * Math.sin(p.y) * Math.sin(p.z);
        pt.y = p.x * Math.cos(p.y);
        GL3DCamera activeCamera = Displayer.getViewport().getCamera();
        GL3DVec3d _pt = activeCamera.getLocalRotation().rotateInverseVector(pt);
        return _pt;
    }

    public void interpolatedDraw(GL2 gl, GL3DVec3d p1s, GL3DVec3d p2s) {
        int subdivisions = 5;

        for (double i = 0; i <= subdivisions; i++) {
            double t = i / subdivisions;
            double y = (1 - t) * p1s.y + t * p2s.y;
            double z = (1 - t) * p1s.z + t * p2s.z;
            GL3DVec3d pc = toCart(new GL3DVec3d(1., y, z));
            gl.glVertex3f((float) pc.x, (float) pc.y, (float) pc.z);
        }
    }

    @Override
    public void mousePressed(MouseEvent e, GL3DCamera camera) {
        GL3DVec3d pt = camera.getVectorFromSphere(e.getPoint());

        if (pt != null) {
            this.zoomBoxStartPoint = (pt);
        }
        Displayer.display();
    }

    @Override
    public void mouseDragged(MouseEvent e, GL3DCamera camera) {
        GL3DVec3d pt = camera.getVectorFromSphere(e.getPoint());
        if (pt != null) {
            this.zoomBoxEndPoint = (pt);
            Displayer.display();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e, GL3DCamera camera) {
        if (zoomBoxStartPoint != null && zoomBoxEndPoint != null) {
            rectangleStartPoints.add(zoomBoxStartPoint);
            rectangleEndPoints.add(zoomBoxEndPoint);
            activeIndex = rectangleEndPoints.size() - 1;
        }

        this.zoomBoxEndPoint = null;
        this.zoomBoxStartPoint = null;
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
        }
        if (e.getKeyCode() == KeyEvent.VK_N) {
            activeIndex++;
            if (activeIndex >= rectangleEndPoints.size()) {
                activeIndex = 0;
            }
        }
        Displayer.display();
    }

}
