package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

public class GLMatrix {

    private static final FloatBuffer fb = BufferUtils.newFloatBuffer(16);

    private static final Matrix4fStack proj = new Matrix4fStack(2);
    private static final Matrix4fStack view = new Matrix4fStack(3);
    private static final Matrix4f mul = new Matrix4f();
    private static final Matrix4f projView = new Matrix4f();

    public static void setOrthoProj(float left, float right, float bottom, float top, float zNear, float zFar) {
        proj.setOrtho(left, right, bottom, top, zNear, zFar);
    }

    public static void pushProj() {
        proj.pushMatrix();
    }

    public static void popProj() {
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

    public static void mulView(float[] m) {
        view.mulAffine(mul.set(m));
    }

    public static void push() {
        view.pushMatrix();
    }

    public static void pop() {
        view.popMatrix();
    }

    static FloatBuffer get() {
        proj.mulOrthoAffine(view, projView);
        projView.get(fb);
        return fb;
    }

}
