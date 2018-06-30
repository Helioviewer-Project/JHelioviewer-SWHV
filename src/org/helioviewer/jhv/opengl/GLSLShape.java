package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.log.Log;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;

public class GLSLShape {

    private final int[] vboAttribLens = { 4, 4 };

    private final VBO[] vbos = new VBO[2];
    private int count;
    private boolean inited = false;

    public void setData(GL2 gl, FloatBuffer points, FloatBuffer colors) {
        count = 0;
        int plen = points.limit() / vboAttribLens[0];
        if (plen * vboAttribLens[0] != points.limit() || points.limit() != colors.limit()) {
            Log.error("Something is wrong with the vertices or colors from this GLShape");
            return;
        }
        vbos[0].bindBufferData(gl, points, Buffers.SIZEOF_FLOAT);
        vbos[1].bindBufferData(gl, colors, Buffers.SIZEOF_FLOAT);
        count = plen;
    }

    public void renderPoints(GL2 gl, double factor) {
        if (count == 0)
            return;

        GLSLShapeShader.point.bind(gl);
        GLSLShapeShader.point.setFactor(factor);
        GLSLShapeShader.point.bindParams(gl);

        bindVBOs(gl);
        gl.glDrawArrays(GL2.GL_POINTS, 0, count);
        unbindVBOs(gl);

        GLSLShader.unbind(gl);
    }

    public void renderShape(GL2 gl, int mode) {
        if (count == 0)
            return;

        GLSLShapeShader.shape.bind(gl);
        GLSLShapeShader.shape.bindParams(gl);

        bindVBOs(gl);
        gl.glDrawArrays(mode, 0, count);
        unbindVBOs(gl);

        GLSLShader.unbind(gl);
    }

    private void bindVBOs(GL2 gl) {
        for (VBO vbo : vbos) {
            vbo.bindArray(gl);
        }
    }

    private void unbindVBOs(GL2 gl) {
        for (VBO vbo : vbos) {
            vbo.unbindArray(gl);
        }
    }

    private void initVBOs(GL2 gl) {
        for (int i = 0; i < vboAttribLens.length; i++) {
            vbos[i] = VBO.gen_float_VBO(i, vboAttribLens[i]);
            vbos[i].init(gl);
        }
    }

    private void disposeVBOs(GL2 gl) {
        for (int i = 0; i < vbos.length; i++) {
            if (vbos[i] != null) {
                vbos[i].dispose(gl);
                vbos[i] = null;
            }
        }
    }

    public void init(GL2 gl) {
        if (!inited) {
            initVBOs(gl);
            inited = true;
        }
    }

    public void dispose(GL2 gl) {
        if (inited) {
            disposeVBOs(gl);
            inited = false;
        }
    }

}
