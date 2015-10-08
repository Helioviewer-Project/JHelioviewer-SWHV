package org.helioviewer.jhv.camera.annotate;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.opengl.GLHelper;

import com.jogamp.opengl.GL2;

public class AnnotateCircle extends AbstractAnnotateable {

    private final ArrayList<GL3DVec3d> circleStartPoints = new ArrayList<GL3DVec3d>();
    private final ArrayList<GL3DVec3d> circleEndPoints = new ArrayList<GL3DVec3d>();

    private GL3DVec3d startPoint;
    private GL3DVec3d endPoint;

    public AnnotateCircle(GL3DCamera _camera) {
        super(_camera);
    }

    private void drawCircle(GL2 gl, GL3DVec3d bp, GL3DVec3d ep) {
        double cosf = bp.dot(ep);
        double r = Math.sqrt(1 - cosf * cosf);
        //P = center + r cos(A) (bp x ep) + r sin(A) ep

        GL3DVec3d center = GL3DVec3d.multiply(bp, cosf);
        center.multiply(radius);

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

        GLHelper.lineWidth(gl, lineWidth);

        gl.glColor3f(dragColor[0], dragColor[1], dragColor[2]);
        if (beingDragged()) {
            drawCircle(gl, startPoint, endPoint);
        }

        gl.glColor3f(baseColor[0], baseColor[1], baseColor[2]);
        int sz = circleStartPoints.size();
        for (int i = 0; i < sz; i++) {
            if (i != activeIndex)
                drawCircle(gl, circleStartPoints.get(i), circleEndPoints.get(i));
        }

        gl.glColor3f(activeColor[0], activeColor[1], activeColor[2]);
        if (sz - 1 >= 0)
            drawCircle(gl, (circleStartPoints.get(activeIndex)), (circleEndPoints.get(activeIndex)));

        gl.glEnable(GL2.GL_TEXTURE_2D);
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
        activeIndex = -1;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        GL3DVec3d pt = camera.getVectorFromSphere(e.getPoint());
        if (pt != null) {
            startPoint = pt;
        }
    }

}
