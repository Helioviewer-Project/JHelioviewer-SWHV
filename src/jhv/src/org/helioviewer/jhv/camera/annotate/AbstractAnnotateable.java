package org.helioviewer.jhv.camera.annotate;

import java.awt.Color;

import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.camera.Camera;

public abstract class AbstractAnnotateable implements Annotateable {

    private static final Color colorActive = Color.red;
    private static final Color colorDrag = Color.yellow;
    private static final Color colorBase = Color.blue;

    protected static final float[] activeColor = new float[] { colorActive.getRed() / 255f, colorActive.getGreen() / 255f, colorActive.getBlue() / 255f };
    protected static final float[] dragColor = new float[] { colorDrag.getRed() / 255f, colorDrag.getGreen() / 255f, colorDrag.getBlue() / 255f };
    protected static final float[] baseColor = new float[] { colorBase.getRed() / 255f, colorBase.getGreen() / 255f, colorBase.getBlue() / 255f };

    protected static final double lineWidth = 1;
    protected static final double radius = 1.01;

    protected final Camera camera;

    protected int activeIndex = -1;

    public AbstractAnnotateable(Camera _camera) {
        camera = _camera;
    }

    protected static Vec3 toSpherical(Camera camera, Vec3 _p) {
        Vec3 p = camera.getOrientation().rotateVector(_p);

        Vec3 pt = new Vec3();
        pt.x = p.length();
        pt.y = Math.acos(p.y / pt.x);
        pt.z = Math.atan2(p.x, p.z);

        return pt;
    }

    protected static Vec3 toCart(Camera camera, double x, double y, double z) {
        Vec3 pt = new Vec3();
        pt.z = x * Math.sin(y) * Math.cos(z);
        pt.x = x * Math.sin(y) * Math.sin(z);
        pt.y = x * Math.cos(y);

        return camera.getOrientation().rotateInverseVector(pt);
    }

}
