package org.helioviewer.jhv.camera.annotate;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;

import com.jogamp.opengl.GL2;

public class AnnotateCross extends AbstractAnnotateable {

    private final ArrayList<Vec3> crossPoints = new ArrayList<Vec3>();

    public AnnotateCross(Camera _camera) {
        super(_camera);
    }

    private void drawCross(GL2 gl, Vec3 bp) {
        double delta = Math.PI * 2.5 / 180;
        Vec3 p1 = new Vec3(radius, bp.y - delta, bp.z);
        Vec3 p2 = new Vec3(radius, bp.y + delta, bp.z);
        Vec3 p3 = new Vec3(radius, bp.y, bp.z - delta);
        Vec3 p4 = new Vec3(radius, bp.y, bp.z + delta);

        gl.glBegin(GL2.GL_LINE_STRIP);
        interpolatedDraw(gl, p1, p2);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_STRIP);
        interpolatedDraw(gl, p3, p4);
        gl.glEnd();
    }

    private void interpolatedDraw(GL2 gl, Vec3 p1s, Vec3 p2s) {
        int subdivisions = 2;
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
    public void render(GL2 gl) {
        if (crossPoints.size() == 0)
            return;

        gl.glLineWidth(lineWidth);

        int sz = crossPoints.size();
        for (int i = 0; i < sz; i++) {
            if (i != activeIndex)
                gl.glColor3f(baseColor[0], baseColor[1], baseColor[2]);
            else
                gl.glColor3f(activeColor[0], activeColor[1], activeColor[2]);
            drawCross(gl, toSpherical(crossPoints.get(i)));
        }
    }

    @Override
    public void clear() {
        crossPoints.clear();
        activeIndex = -1;
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
    public void mousePressed(MouseEvent e) {
        Vec3 pt = CameraHelper.getVectorFromSphere(camera, Displayer.getActiveViewport(), e.getPoint());
        if (pt != null) {
            crossPoints.add(pt);
            activeIndex = crossPoints.size() - 1;
            Displayer.display();
        }
    }

}
