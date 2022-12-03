package org.helioviewer.jhv.math;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

public class Transform {

    private static final FloatBuffer fb = BufferUtils.newFloatBuffer(16);
    private static final FloatBuffer mvp = BufferUtils.newFloatBuffer(16);

    private static final Matrix4fStack proj = new Matrix4fStack(2);
    private static final Matrix4fStack view = new Matrix4fStack(3);
    private static final Matrix4f mul = new Matrix4f();

    private static int projDepth;
    private static int viewDepth;

    public static void pushProjection() {
        proj.pushMatrix();
        projDepth++;
    }

    public static void popProjection() {
        proj.popMatrix();
        projDepth--;
    }

    public static void pushView() {
        view.pushMatrix();
        viewDepth++;
    }

    public static void popView() {
        view.popMatrix();
        viewDepth--;
    }

    public static void setOrthoProjection(float left, float right, float bottom, float top, float zNear, float zFar) {
        proj.setOrtho(left, right, bottom, top, zNear, zFar);
    }

    public static void setOrthoSymmetricProjection(float width, float height, float zNear, float zFar) {
        proj.setOrthoSymmetric(width, height, zNear, zFar);
    }

    public static void setIdentityView() {
        view.identity();
    }

    public static void setTranslateView(float x, float y, float z) {
        view.translation(x, y, z);
    }

    public static void mulView(Matrix4f m) {
        view.mulAffine(m);
    }

    public static void rotateView(Quat q) {
        float w = (float) q.a, w2 = w * w;
        float x = (float) q.u.x, x2 = x * x;
        float y = (float) q.u.y, y2 = y * y;
        float z = (float) q.u.z, z2 = z * z;

        view.mulAffine(mul.set(w2 + x2 - y2 - z2, 2 * x * y + 2 * w * z, 2 * x * z - 2 * w * y, 0,
                2 * x * y - 2 * w * z, w2 - x2 + y2 - z2, 2 * y * z + 2 * w * x, 0,
                2 * x * z + 2 * w * y, 2 * y * z - 2 * w * x, w2 - x2 - y2 + z2, 0,
                0, 0, 0, w2 + x2 + y2 + z2));
    }

    public static void rotateViewInverse(Quat q) {
        float w = (float) q.a, w2 = w * w;
        float x = (float) q.u.x, x2 = x * x;
        float y = (float) q.u.y, y2 = y * y;
        float z = (float) q.u.z, z2 = z * z;

        view.mulAffine(mul.set(w2 + x2 - y2 - z2, 2 * x * y - 2 * w * z, 2 * x * z + 2 * w * y, 0,
                2 * x * y + 2 * w * z, w2 - x2 + y2 - z2, 2 * y * z - 2 * w * x, 0,
                2 * x * z - 2 * w * y, 2 * y * z + 2 * w * x, w2 - x2 - y2 + z2, 0,
                0, 0, 0, w2 + x2 + y2 + z2));
    }

    public static void cacheMVP() {
        proj.mulOrthoAffine(view, mul); // assumes ortho
        mul.get(mvp);
    }

    public static FloatBuffer get() {
        if (projDepth == 0 && viewDepth == 0) {
            //System.out.println(">> hit");
            //Thread.dumpStack();
            return mvp;
        }

        proj.mulOrthoAffine(view, mul); // assumes ortho
        mul.get(fb);
        return fb;
    }

}
