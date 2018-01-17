package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.log.Log;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;

public class GLShape {

    private int[] vboAttribRefs;
    private final int[] vboAttribLens = { 4, 4 };

    private final VBO[] vbos = new VBO[2];
    private VBO ivbo;
    private boolean hasPoints = false;

    public void setData(GL2 gl, FloatBuffer points, FloatBuffer colors) {
        hasPoints = false;
        int plen = points.limit() / 4;
        if (plen * 4 != points.limit() || points.limit() != colors.limit()) {
            Log.error("Something is wrong with the vertices or colors from this GLPoint.");
            return;
        }
        vbos[0].bindBufferData(gl, points, Buffers.SIZEOF_FLOAT);
        vbos[1].bindBufferData(gl, colors, Buffers.SIZEOF_FLOAT);

        IntBuffer indexBuffer = gen_indices(plen);
        ivbo.bindBufferData(gl, indexBuffer, Buffers.SIZEOF_INT);

        hasPoints = true;
    }

    private static IntBuffer gen_indices(int plen) {
        IntBuffer indicesBuffer = BufferUtils.newIntBuffer(plen);
        for (int j = 0; j < plen; j++) {
            indicesBuffer.put(j);
        }
        indicesBuffer.rewind();
        return indicesBuffer;
    }

    public void renderPoints(GL2 gl, double factor) {
        if (!hasPoints)
            return;

        GLSLShapeShader.point.bind(gl);
        GLSLShapeShader.point.setFactor(factor);
        GLSLShapeShader.point.bindParams(gl);

        bindVBOs(gl);
        gl.glDrawElements(GL2.GL_POINTS, ivbo.bufferSize, GL2.GL_UNSIGNED_INT, 0);
        unbindVBOs(gl);

        GLSLShader.unbind(gl);
    }

    public void renderShape(GL2 gl, int mode) {
        if (!hasPoints)
            return;

        GLSLShapeShader.shape.bind(gl);

        bindVBOs(gl);
        gl.glDrawElements(mode, ivbo.bufferSize, GL2.GL_UNSIGNED_INT, 0);
        unbindVBOs(gl);

        GLSLShader.unbind(gl);
    }

    private void bindVBOs(GL2 gl) {
        for (VBO vbo : vbos) {
            vbo.bindArray(gl);
        }
        ivbo.bindArray(gl);
    }

    private void unbindVBOs(GL2 gl) {
        for (VBO vbo : vbos) {
            vbo.unbindArray(gl);
        }
        ivbo.unbindArray(gl);
    }

    private void initVBOs(GL2 gl) {
        for (int i = 0; i < vboAttribRefs.length; i++) {
            vbos[i] = VBO.gen_float_VBO(vboAttribRefs[i], vboAttribLens[i]);
            vbos[i].init(gl);
        }
        ivbo = VBO.gen_index_VBO();
        ivbo.init(gl);
    }

    private void disposeVBOs(GL2 gl) {
        for (int i = 0; i < vbos.length; i++) {
            if (vbos[i] != null) {
                vbos[i].dispose(gl);
                vbos[i] = null;
            }
        }
        if (ivbo != null) {
            ivbo.dispose(gl);
            ivbo = null;
        }
    }

    public void init(GL2 gl) {
        vboAttribRefs = new int[] { GLSLShapeShader.positionRef, GLSLShapeShader.colorRef };
        initVBOs(gl);
    }

    public void dispose(GL2 gl) {
        disposeVBOs(gl);
    }

}
