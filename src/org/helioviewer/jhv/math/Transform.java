package org.helioviewer.jhv.math;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

public class Transform {

    private static final FloatBuffer fb = BufferUtils.newFloatBuffer(16);

    private static final Matrix4fStack proj = new Matrix4fStack(2);
    private static final Matrix4fStack view = new Matrix4fStack(3);
    private static final Matrix4f mul = new Matrix4f();

    public static void setOrthoProjection(float left, float right, float bottom, float top, float zNear, float zFar) {
        proj.setOrtho(left, right, bottom, top, zNear, zFar);
    }

    public static void pushProjection() {
        proj.pushMatrix();
    }

    public static void popProjection() {
        proj.popMatrix();
    }

    public static void setView(float[] m) {
        view.set(m);
    }

    public static void setIdentityView() {
        view.identity();
    }

    public static void setTranslateView(float x, float y, float z) {
        view.translation(x, y, z);
    }

    public static void translateView(float x, float y, float z) {
        view.translate(x, y, z);
    }

    public static void rotateView(float ang, float x, float y, float z) { // degrees, axis normalized
        view.rotate((float) (Math.PI / 180.) * ang, x, y, z);
    }

    public static void rotateView(Quat q) {
        float w = (float) q.a, w2 = w * w;
        float x = (float) q.u.x, x2 = x * x;
        float y = (float) q.u.y, y2 = y * y;
        float z = (float) q.u.z, z2 = z * z;

        view.mulAffine(mul.set(w2 + x2 - y2 - z2,     2 * x * y + 2 * w * z, 2 * x * z - 2 * w * y, 0,
                               2 * x * y - 2 * w * z, w2 - x2 + y2 - z2,     2 * y * z + 2 * w * x, 0,
                               2 * x * z + 2 * w * y, 2 * y * z - 2 * w * x, w2 - x2 - y2 + z2,     0,
                               0,                     0,                     0,                     w2 + x2 + y2 + z2));
    }

    public static void rotateViewInverse(Quat q) {
        float w = (float) q.a, w2 = w * w;
        float x = (float) q.u.x, x2 = x * x;
        float y = (float) q.u.y, y2 = y * y;
        float z = (float) q.u.z, z2 = z * z;

        view.mulAffine(mul.set(w2 + x2 - y2 - z2,     2 * x * y - 2 * w * z, 2 * x * z + 2 * w * y, 0,
                               2 * x * y + 2 * w * z, w2 - x2 + y2 - z2,     2 * y * z - 2 * w * x, 0,
                               2 * x * z - 2 * w * y, 2 * y * z + 2 * w * x, w2 - x2 - y2 + z2,     0,
                               0,                     0,                     0,                     w2 + x2 + y2 + z2));
    }

    public static void pushView() {
        view.pushMatrix();
    }

    public static void popView() {
        view.popMatrix();
    }

    public static FloatBuffer get() {
        proj.mulOrthoAffine(view, mul); // assumes ortho
        mul.get(fb);
        return fb;
    }

}
