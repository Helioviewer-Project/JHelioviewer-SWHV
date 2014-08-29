package org.helioviewer.swhv;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.VectorUtil;

public class QuaternionExtension extends Quaternion {
    public void fromAxis(float[] vector, float angle) {
        float sin = FloatUtil.sin(angle *= 0.5f);
        float[] nv = VectorUtil.normalizeVec3(vector);
        setX((nv[0] * sin));
        setY((nv[1] * sin));
        setZ((nv[2] * sin));
        setW(FloatUtil.cos(angle));
    }
}
