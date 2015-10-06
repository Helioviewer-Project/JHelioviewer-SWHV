package org.helioviewer.jhv.camera.annotateable;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.display.Displayer;

import com.jogamp.opengl.GL2;

public class GL3DAnnotateCircle implements GL3DAnnotatable {

    private final ArrayList<GL3DVec3d> circleStartPoints = new ArrayList<GL3DVec3d>();
    private final ArrayList<GL3DVec3d> circleEndPoints = new ArrayList<GL3DVec3d>();
    private int activeIndex = -1;
    private static final double radius = 1.01;
    private GL3DVec3d startPoint;
    private GL3DVec3d endPoint;

    private void drawCircle(GL2 gl, GL3DVec3d bp, GL3DVec3d ep) {
        double cosf = bp.dot(ep);
        double r = Math.sqrt(1 - cosf * cosf);
        //P = center + r cos(A) (bp x ep) + r sin(A) ep

        GL3DVec3d center = GL3DVec3d.multiply(bp, cosf);
        GL3DVec3d u = GL3DVec3d.cross(bp, ep);
        u.normalize();
        GL3DVec3d v = GL3DVec3d.cross(bp, u);
        gl.glBegin(GL2.GL_LINE_STRIP);
        int subdivs = 100;

        for (int i = 0; i <= subdivs; i++) {
            double t = i * 2. * Math.PI / subdivs;
            double cosr = Math.cos(t) * r;
            double sinr = Math.sin(t) * r;
            float x = (float) (center.x + cosr * u.x + sinr * v.x);
            float y = (float) (center.y + cosr * u.y + sinr * v.y);
            float z = (float) (center.z + cosr * u.z + sinr * v.z);
            gl.glVertex3f(x, y, z);
        }
        gl.glEnd();
    }

    private boolean beingDragged() {
        return endPoint != null && startPoint != null;
    }

    @Override
    public void render(GL2 gl) {
        if (circleStartPoints.size() == 0 && !beingDragged())
            return;

        gl.glDisable(GL2.GL_TEXTURE_2D);

        gl.glLineWidth(2.0f);

        gl.glColor3f(GL3DAnnotatable.dragColor.getRed() / 255f, GL3DAnnotatable.dragColor.getGreen() / 255f, GL3DAnnotatable.dragColor.getBlue() / 255f);
        if (beingDragged()) {
            drawCircle(gl, startPoint, endPoint);
        }

        gl.glColor3f(GL3DAnnotatable.baseColor.getRed() / 255f, GL3DAnnotatable.baseColor.getGreen() / 255f, GL3DAnnotatable.baseColor.getBlue() / 255f);
        int sz = circleStartPoints.size();
        for (int i = 0; i < sz; i++) {
            if (i != activeIndex)
                drawCircle(gl, circleStartPoints.get(i), circleEndPoints.get(i));
        }

        gl.glColor3f(GL3DAnnotatable.activeColor.getRed() / 255f, GL3DAnnotatable.activeColor.getGreen() / 255f, GL3DAnnotatable.activeColor.getBlue() / 255f);
        if (sz - 1 >= 0)
            drawCircle(gl, (circleStartPoints.get(activeIndex)), (circleEndPoints.get(activeIndex)));

        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        GL3DVec3d pt = Displayer.getViewport().getCamera().getVectorFromSphere(e.getPoint());
        if (pt != null) {
            endPoint = pt;
            Displayer.display();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (beingDragged()) {
            circleStartPoints.add(startPoint);
            circleEndPoints.add(endPoint);
            activeIndex = circleEndPoints.size() - 1;
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
                circleEndPoints.remove(activeIndex);
                circleStartPoints.remove(activeIndex);
            }
            activeIndex = circleEndPoints.size() - 1;
            Displayer.display();
        } else if (code == KeyEvent.VK_N) {
            activeIndex++;
            if (activeIndex >= circleEndPoints.size()) {
                activeIndex = 0;
            }
            Displayer.display();
        }
    }

    @Override
    public void reset() {
        circleStartPoints.clear();
        circleEndPoints.clear();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        GL3DVec3d pt = Displayer.getViewport().getCamera().getVectorFromSphere(e.getPoint());
        if (pt != null) {
            startPoint = pt;
        }
    }

}
