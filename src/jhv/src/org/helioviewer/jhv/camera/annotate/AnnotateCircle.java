package org.helioviewer.jhv.camera.annotate;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;

import com.jogamp.opengl.GL2;

public class AnnotateCircle extends AbstractAnnotateable {

    private Vec3 circleStartPoint;
    private Vec3 circleEndPoint;

    private Vec3 startPoint;
    private Vec3 endPoint;

    public AnnotateCircle(Camera _camera) {
        super(_camera);
    }

    private void drawCircle(GL2 gl, Vec3 bp, Vec3 ep) {
        double cosf = bp.dot(ep);
        double r = Math.sqrt(1 - cosf * cosf);
        // P = center + r cos(A) (bp x ep) + r sin(A) ep

        Vec3 center = Vec3.multiply(bp, cosf);
        center.multiply(radius);

        Vec3 u = Vec3.cross(bp, ep);
        u.normalize();
        Vec3 v = Vec3.cross(bp, u);

        gl.glBegin(GL2.GL_LINE_STRIP);
        int subdivs = 100;
        for (int i = 0; i <= subdivs; i++) {
            double t = i * 2. * Math.PI / subdivs;
            double cosr = Math.cos(t) * r;
            double sinr = Math.sin(t) * r;
            float x = (float) (center.x + cosr * u.x + sinr * v.x);
            float y = (float) (center.y + cosr * u.y + sinr * v.y);
            float z = (float) (center.z + cosr * u.z + sinr * v.z);

            if (Displayer.mode != Displayer.DisplayMode.ORTHO) {
                Vec3 pt = camera.getViewpoint().orientation.rotateVector(new Vec3(x, -y, z));
                Vec2 tf = GridScale.current.transform(pt);

                gl.glVertex2f((float) (tf.x * Displayer.getActiveViewport().aspect), (float) tf.y);
            } else {
                gl.glVertex3f(x, y, z);
            }
        }
        gl.glEnd();
    }

    @Override
    public boolean beingDragged() {
        return endPoint != null && startPoint != null;
    }

    @Override
    public void render(GL2 gl, boolean active) {
        if ((circleStartPoint == null || circleEndPoint == null) && !beingDragged())
            return;

        gl.glLineWidth(lineWidth);

        gl.glColor3f(dragColor[0], dragColor[1], dragColor[2]);
        if (beingDragged()) {
            drawCircle(gl, startPoint, endPoint);
        }
        else {
            if (active)
                gl.glColor3f(activeColor[0], activeColor[1], activeColor[2]);
            else
                gl.glColor3f(baseColor[0], baseColor[1], baseColor[2]);

            drawCircle(gl, circleStartPoint, circleEndPoint);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Vec3 pt = computePoint(e);
        if (pt != null) {
            endPoint = pt;
            Displayer.display();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (beingDragged()) {
            circleStartPoint = startPoint;
            circleEndPoint = endPoint;
        }

        endPoint = null;
        startPoint = null;
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Vec3 pt = computePoint(e);
        if (pt != null) {
            startPoint = pt;
        }
    }

}
