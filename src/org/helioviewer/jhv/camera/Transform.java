package org.helioviewer.jhv.camera;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.math.Quat;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

public class Transform {

    private static final FloatBuffer fb = BufferUtils.newFloatBuffer(16);
    private static final FloatBuffer mvp = BufferUtils.newFloatBuffer(16);

    private static final Matrix4fStack proj = new Matrix4fStack(2);
    private static final Matrix4fStack view = new Matrix4fStack(3);
    private static final Matrix4f mul = new Matrix4f();
    private static final Quaternionf quat = new Quaternionf();

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

    public static void setOrtho2DProjection(float left, float right, float bottom, float top) {
        proj.setOrtho2D(left, right, bottom, top);
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
        view.rotateAffine(quat.set((float) q.x, (float) q.y, (float) q.z, (float) q.w));
    }

    public static void rotateViewInverse(Quat q) {
        view.rotateAffine(quat.set((float) q.x, (float) q.y, (float) q.z, (float) -q.w));
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
