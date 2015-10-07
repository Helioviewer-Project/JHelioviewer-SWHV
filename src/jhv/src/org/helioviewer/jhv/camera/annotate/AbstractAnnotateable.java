package org.helioviewer.jhv.camera.annotate;

import java.awt.Color;

import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.camera.GL3DCamera;

public abstract class AbstractAnnotateable implements Annotateable {

    protected static final float[] activeColor = new float[] { Color.red.getRed() / 255f, Color.red.getGreen() / 255f, Color.red.getBlue() / 255f };
    protected static final float[] dragColor = new float[] { Color.yellow.getRed() / 255f, Color.yellow.getGreen() / 255f, Color.yellow.getBlue() / 255f };
    protected static final float[] baseColor = new float[] { Color.blue.getRed() / 255f, Color.blue.getGreen() / 255f, Color.blue.getBlue() / 255f };

    protected static final double radius = 1.01;

    protected final GL3DCamera camera;

    protected int activeIndex = -1;

    public AbstractAnnotateable(GL3DCamera _camera) {
        camera = _camera;
    }

    protected static GL3DVec3d toSpherical(GL3DCamera camera, GL3DVec3d _p) {
        GL3DVec3d p = camera.getLocalRotation().rotateVector(_p);

        GL3DVec3d pt = new GL3DVec3d();
        pt.x = p.length();
        pt.y = Math.acos(p.y / pt.x);
        pt.z = Math.atan2(p.x, p.z);

        return pt;
    }

    protected static GL3DVec3d toCart(GL3DCamera camera, double x, double y, double z) {
        GL3DVec3d pt = new GL3DVec3d();
        pt.z = x * Math.sin(y) * Math.cos(z);
        pt.x = x * Math.sin(y) * Math.sin(z);
        pt.y = x * Math.cos(y);

        return camera.getLocalRotation().rotateInverseVector(pt);
    }

}
