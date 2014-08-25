package org.helioviewer.swhv;

import javax.media.opengl.GL3;

import org.helioviewer.gl3d.scenegraph.math.GL3DMat4f;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3f;

public class DefaultCamera implements Camera {
    GL3DMat4f viewMatrix;
    GL3DMat4f projectionMatrix;
    GL3DVec3f eye;
    GL3DVec3f right;
    GL3DVec3f up;

    private int width = 1;
    private int height = 1;
    private float aspect;
    private final float clipNear;
    private final float clipFar;
    private final float fov;

    public DefaultCamera(float fov, float clipNear, float clipFar, GL3DVec3f eye, GL3DVec3f right, GL3DVec3f up) {
        this.clipNear = clipNear;
        this.clipFar = clipFar;
        this.fov = fov;
        this.eye = eye;
        this.right = right;
        this.up = up;
        computeViewMatrix();
    }

    private void computeViewMatrix() {
        GL3DVec3f forward = GL3DVec3f.cross(up, right);
        viewMatrix = new GL3DMat4f(right.x, right.y, right.z, -GL3DVec3f.dot(right, eye), up.x, up.y, up.z, -GL3DVec3f.dot(up, eye), -forward.x, -forward.y, -forward.z, GL3DVec3f.dot(forward, eye), 0, 0, 0, 1);
    }

    public void createPerspective(GL3 gl) {
        this.aspect = (1.f * width / height);
        this.projectionMatrix = GL3DMat4f.perspective(this.fov, aspect, this.clipNear, this.clipFar);
    }

    public GL3DMat4f getViewMatrix() {
        return this.viewMatrix;
    }

    @Override
    public GL3DMat4f getViewProjectionMatrix(GL3 gl) {
        createPerspective(gl);
        GL3DMat4f helpMatrix = this.projectionMatrix.copy();
        helpMatrix.multiply(this.viewMatrix);
        return helpMatrix;
    }

    @Override
    public void reshape(int w, int h) {
        this.width = w;
        this.height = h;
    }

}
