package org.helioviewer.jhv.camera.annotateable;

import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.camera.GL3DCamera;

import com.jogamp.opengl.GL3;

public abstract class GL3DAbstractAnnotatable implements GL3DAnnotatable {

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
