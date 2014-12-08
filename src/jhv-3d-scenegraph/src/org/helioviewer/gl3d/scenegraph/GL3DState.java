package org.helioviewer.gl3d.scenegraph;

import java.util.Date;
import java.util.Stack;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;

/**
 * The {@link GL3DState} is recreated every render pass by the
 * {@link GL3DComponentView}. It provides the reference to the {@link GL2}
 * object and stores some globally relevant information such as width and height
 * of the viewport, etc. Also it allows for the stacking of the view
 * transformations.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DState {
    private static GL3DState instance;

    public GL2 gl;

    public GL3DMat4d mv;
    private final Stack<GL3DMat4d> matrixStack;

    protected GL3DMat4d mvInverse;

    protected GL3DMat3d normalMatrix;

    protected GL3DCamera activeCamera;

    protected int viewportWidth;
    protected int viewportHeight;

    private Date currentObservationDate;

    public static GL3DState create(GL2 gl) {
        instance = new GL3DState(gl);
        return instance;
    }

    public static GL3DState get() {
        return instance;
    }

    public static GL3DState getUpdated(GL2 gl, int width, int height) {
        instance.gl = gl;
        instance.viewportWidth = width;
        instance.viewportHeight = height;
        return instance;
    }

    private GL3DState(GL2 gl) {
        this.gl = gl;
        this.mv = GL3DMat4d.identity();
        this.matrixStack = new Stack<GL3DMat4d>();
    }

    public void pushMV() {
        gl.glPushMatrix();
        this.matrixStack.push(new GL3DMat4d(this.mv));
        // Log.debug("GL3DState.pushMV: "+this.matrixStack.size());
    }

    public void popMV() {
        gl.glPopMatrix();
        this.mv = this.matrixStack.pop();
        // Log.debug("GL3DState.popMV: "+this.matrixStack.size());
    }

    public void loadIdentity() {
        this.mv = GL3DMat4d.identity();
        this.mvInverse = GL3DMat4d.identity();
        this.matrixStack.push(new GL3DMat4d(this.mv));
        this.gl.glLoadIdentity();
    }

    public GL3DMat4d multiplyMV(GL3DMat4d m) {
        this.mv.multiply(m);
        gl.glMultMatrixd(m.m, 0);
        return mv;
    }

    public void buildInverseAndNormalMatrix() {
        try {
            this.mvInverse = this.mv.inverse();
            this.normalMatrix = this.mvInverse.mat3().transpose();
        } catch (IllegalArgumentException e) {
            // TODO: What to do when matrix cannot be inverted?
            Log.error("Cannot Invert ModelView Matrix! Singularity occurred!", e);
            this.mvInverse = GL3DMat4d.identity();
            this.normalMatrix = new GL3DMat3d();
            this.mv = GL3DMat4d.identity();
        }
    }

    public GL3DMat4d getMVInverse() {
        return new GL3DMat4d(this.mvInverse);
    }

    public GL3DMat4d getMV() {
        return new GL3DMat4d(this.mv);
    }

    public boolean checkGLErrors(String message) {
        if (gl == null) {
            Log.warn("OpenGL not yet Initialised!");
            return true;
        }
        int glErrorCode = gl.glGetError();

        if (glErrorCode != GL2.GL_NO_ERROR) {
            GLU glu = new GLU();
            Log.error("GL Error (" + glErrorCode + "): " + glu.gluErrorString(glErrorCode) + " - @" + message);
            if (glErrorCode == GL2.GL_INVALID_OPERATION) {
                // Find the error position
                int[] err = new int[1];
                gl.glGetIntegerv(GL2.GL_PROGRAM_ERROR_POSITION_ARB, err, 0);
                if (err[0] >= 0) {
                    String error = gl.glGetString(GL2.GL_PROGRAM_ERROR_STRING_ARB);
                    Log.error("GL error at " + err[0] + ":\n" + error);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean checkGLErrors() {
        return checkGLErrors(this.gl);
    }

    public boolean checkGLErrors(GL2 gl) {
        if (gl == null) {
            Log.warn("OpenGL not yet Initialised!");
            return true;
        }
        int glErrorCode = gl.glGetError();

        if (glErrorCode != GL2.GL_NO_ERROR) {
            GLU glu = new GLU();
            Log.error("GL Error (" + glErrorCode + "): " + glu.gluErrorString(glErrorCode));
            if (glErrorCode == GL2.GL_INVALID_OPERATION) {
                // Find the error position
                int[] err = new int[1];
                gl.glGetIntegerv(GL2.GL_PROGRAM_ERROR_POSITION_ARB, err, 0);
                if (err[0] >= 0) {
                    String error = gl.glGetString(GL2.GL_PROGRAM_ERROR_STRING_ARB);
                    Log.error("GL error at " + err[0] + ":\n" + error);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public void setActiveChamera(GL3DCamera camera) {
        this.activeCamera = camera;
    }

    public GL3DCamera getActiveCamera() {
        return activeCamera;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public Date getCurrentObservationDate() {
        return currentObservationDate;
    }

    public void setCurrentObservationDate(Date currentObservationDate) {
        this.currentObservationDate = currentObservationDate;
    }
}
