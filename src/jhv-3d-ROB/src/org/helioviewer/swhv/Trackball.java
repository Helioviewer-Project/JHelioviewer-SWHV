package org.helioviewer.swhv;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.vecmath.Vector3f;

import com.jogamp.opengl.math.Quaternion;

public class Trackball extends MouseAdapter implements MouseMotionListener {

    public final static float TRACKBALLSIZE = 1f;

    QuaternionExtension curquat = new QuaternionExtension();
    QuaternionExtension lastquat = new QuaternionExtension();
    int beginx, beginy;

    int trackball_width;
    int trackball_height;
    int trackball_button;

    boolean trackball_tracking = false;

    public void trackballInit(int button) {
        this.trackball_button = button;
        trackball(this.curquat, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public float[] trackballMatrix() {
        Quaternion q = new Quaternion(this.curquat);
        q.normalize();
        return q.toMatrix(new float[16], 0);
    }

    public void trackballReshape(int width, int height) {
        trackball_width = width;
        trackball_height = height;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (trackball_tracking) {
            trackball(lastquat, (2.0f * beginx - trackball_width) / trackball_width / 2, (trackball_height - 2.0f * beginy) / trackball_height / 2, (2.0f * e.getX() - trackball_width) / trackball_width/2, (trackball_height - 2.0f * e.getY()) / trackball_height/2);
            beginx = e.getX();
            beginy = e.getY();
            this.curquat.mult(this.lastquat);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    void trackballStartMotion(int x, int y) {
        trackball_tracking = true;
        beginx = x;
        beginy = y;
    }

    void trackballStopMotion() {
        trackball_tracking = false;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        trackballStartMotion(e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        trackballStopMotion();
    }

    void trackball(QuaternionExtension q, float p1x, float p1y, float p2x, float p2y) {
        Vector3f a;
        float phi;
        Vector3f p1, p2, d;
        float t;

        if (p1x == p2x && p1y == p2y) {
            q.setZ(0.f);
            q.setW(1.f);
            return;
        }

        p1 = new Vector3f(p1x, p1y, trackball_project_to_sphere(TRACKBALLSIZE, p1x, p1y));
        p2 = new Vector3f(p2x, p2y, trackball_project_to_sphere(TRACKBALLSIZE, p2x, p2y));

        a = new Vector3f();
        a.cross(p2, p1);

        d = new Vector3f();
        d.sub(p1, p2);
        t = d.length() / (2.0f * TRACKBALLSIZE);

        if (t > 1.0)
            t = 1.0f;
        if (t < -1.0)
            t = -1.0f;
        phi = 2.0f * (float) Math.asin(t);
        float[] fl = new float[3];
        fl[0] = a.x;
        fl[1] = a.y;
        fl[2] = a.z;
        q.fromAxis(fl, phi);
    }

    private static float trackball_project_to_sphere(float r, float x, float y) {
        float d, t, z;

        d = (float) Math.sqrt(x * x + y * y);
        if (d < r * 0.70710678118654752440) { /* Inside sphere */
            z = (float) Math.sqrt(r * r - d * d);
        } else { /* On hyperbola */
            t = r / 1.41421356237309504880f;
            z = t * t / d;
        }
        return -z;
    }
}
