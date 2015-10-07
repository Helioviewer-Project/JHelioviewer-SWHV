package org.helioviewer.jhv.camera.annotateable;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.camera.GL3DCamera;

import com.jogamp.opengl.GL2;

public interface GL3DAnnotatable {

    public static final Color activeColor = Color.red;
    public static final Color dragColor = Color.yellow;
    public static final Color baseColor = Color.blue;

    public static final double radius = 1.01;

    public void render(GL2 gl);

    public void mouseDragged(MouseEvent e);

    public void mouseReleased(MouseEvent e);

    public void keyPressed(KeyEvent e);

    public void reset();

    public void mousePressed(MouseEvent e);

    public static GL3DVec3d toSpherical(GL3DCamera camera, GL3DVec3d _p) {
        GL3DVec3d p = camera.getLocalRotation().rotateVector(_p);

        GL3DVec3d pt = new GL3DVec3d();
        pt.x = p.length();
        pt.y = Math.acos(p.y / pt.x);
        pt.z = Math.atan2(p.x, p.z);

        return pt;
    }

    public static GL3DVec3d toCart(GL3DCamera camera, double x, double y, double z) {
        GL3DVec3d pt = new GL3DVec3d();
        pt.z = x * Math.sin(y) * Math.cos(z);
        pt.x = x * Math.sin(y) * Math.sin(z);
        pt.y = x * Math.cos(y);

        return camera.getLocalRotation().rotateInverseVector(pt);
    }
}
