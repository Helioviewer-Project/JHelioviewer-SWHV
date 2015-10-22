package org.helioviewer.jhv.camera.annotate;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.helioviewer.jhv.base.math.Vec3d;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.opengl.GLHelper;

import com.jogamp.opengl.GL2;

public class AnnotateCross extends AbstractAnnotateable {

    private final ArrayList<Vec3d> crossPoints = new ArrayList<Vec3d>();

    public AnnotateCross(GL3DCamera _camera) {
        super(_camera);
    }

    private void drawCross(GL2 gl, Vec3d bp) {
        double delta = Math.PI * 2.5 / 180;
        Vec3d p1 = new Vec3d(radius, bp.y - delta, bp.z);
        Vec3d p2 = new Vec3d(radius, bp.y + delta, bp.z);
        Vec3d p3 = new Vec3d(radius, bp.y, bp.z - delta);
        Vec3d p4 = new Vec3d(radius, bp.y, bp.z + delta);

        gl.glBegin(GL2.GL_LINE_STRIP);
        interpolatedDraw(gl, p1, p2);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_STRIP);
        interpolatedDraw(gl, p3, p4);
        gl.glEnd();
    }

    private void interpolatedDraw(GL2 gl, Vec3d p1s, Vec3d p2s) {
        int subdivisions = 2;

        for (double i = 0; i <= subdivisions; i++) {
            double t = i / subdivisions;
            double y = (1 - t) * p1s.y + t * p2s.y;
            double z = (1 - t) * p1s.z + t * p2s.z;
            Vec3d pc = toCart(camera, radius, y, z);
            gl.glVertex3f((float) pc.x, (float) pc.y, (float) pc.z);
        }
    }

    @Override
    public void render(GL2 gl) {
        if (crossPoints.size() == 0)
            return;

        GLHelper.lineWidth(gl, lineWidth);

        gl.glColor3f(baseColor[0], baseColor[1], baseColor[2]);
        int sz = crossPoints.size();
        for (int i = 0; i < sz; i++) {
            if (i != activeIndex)
                drawCross(gl, toSpherical(camera, crossPoints.get(i)));
        }

        gl.glColor3f(activeColor[0], activeColor[1], activeColor[2]);
        if (sz - 1 >= 0)
            drawCross(gl, toSpherical(camera, crossPoints.get(activeIndex)));
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
            if (activeIndex >= 0) {
                activeIndex++;
                activeIndex = activeIndex % crossPoints.size();
                Displayer.display();
            }
        }
    }

    @Override
    public void reset() {
        crossPoints.clear();
        activeIndex = -1;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Vec3d pt = camera.getVectorFromSphere(e.getPoint());
        if (pt != null) {
            crossPoints.add(pt);
            activeIndex = crossPoints.size() - 1;
            Displayer.display();
        }
    }

}
