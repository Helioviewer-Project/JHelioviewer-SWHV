package org.helioviewer.jhv.camera.annotateable;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;

import com.jogamp.opengl.GL2;

public class GL3DAnnotateCross extends GL3DAbstractAnnotatable {

    private final ArrayList<GL3DVec3d> crossPoints = new ArrayList<GL3DVec3d>();
    private int activeIndex = -1;

    private final GL3DCamera camera;

    public GL3DAnnotateCross(GL3DCamera _camera) {
        camera = _camera;
    }

    private void drawCross(GL2 gl, GL3DVec3d bp) {
        double delta = Math.PI * 2.5 / 180;
        GL3DVec3d p1 = new GL3DVec3d(radius, bp.y - delta, bp.z);
        GL3DVec3d p2 = new GL3DVec3d(radius, bp.y + delta, bp.z);
        GL3DVec3d p3 = new GL3DVec3d(radius, bp.y, bp.z - delta);
        GL3DVec3d p4 = new GL3DVec3d(radius, bp.y, bp.z + delta);

        gl.glBegin(GL2.GL_LINE_STRIP);
        interpolatedDraw(gl, p1, p2);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_STRIP);
        interpolatedDraw(gl, p3, p4);
        gl.glEnd();
    }

    private void interpolatedDraw(GL2 gl, GL3DVec3d p1s, GL3DVec3d p2s) {
        int subdivisions = 2;

        for (double i = 0; i <= subdivisions; i++) {
            double t = i / subdivisions;
            double y = (1 - t) * p1s.y + t * p2s.y;
            double z = (1 - t) * p1s.z + t * p2s.z;
            GL3DVec3d pc = toCart(camera, radius, y, z);
            gl.glVertex3f((float) pc.x, (float) pc.y, (float) pc.z);
        }
    }

    @Override
    public void render(GL2 gl) {
        if (crossPoints.size() == 0)
            return;

        gl.glDisable(GL2.GL_TEXTURE_2D);

        gl.glLineWidth(2.0f);

        gl.glColor3f(GL3DAnnotatable.baseColor.getRed() / 255f, GL3DAnnotatable.baseColor.getGreen() / 255f, GL3DAnnotatable.baseColor.getBlue() / 255f);
        int sz = crossPoints.size();
        for (int i = 0; i < sz; i++) {
            if (i != activeIndex)
                drawCross(gl, toSpherical(camera, crossPoints.get(i)));
        }

        gl.glColor3f(GL3DAnnotatable.activeColor.getRed() / 255f, GL3DAnnotatable.activeColor.getGreen() / 255f, GL3DAnnotatable.activeColor.getBlue() / 255f);
        if (sz - 1 >= 0)
            drawCross(gl, toSpherical(camera, crossPoints.get(activeIndex)));

        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE) {
            if (activeIndex >= 0) {
                crossPoints.remove(activeIndex);
            }
            activeIndex = crossPoints.size() - 1;
            Displayer.display();
        } else if (code == KeyEvent.VK_N) {
            activeIndex++;
            if (activeIndex >= crossPoints.size()) {
                activeIndex = 0;
            }
            Displayer.display();
        }
    }

    @Override
    public void reset() {
        crossPoints.clear();
        activeIndex = -1;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        GL3DVec3d pt = camera.getVectorFromSphere(e.getPoint());
        if (pt != null) {
            crossPoints.add(pt);
            activeIndex = crossPoints.size() - 1;
            Displayer.display();
        }
    }

}
