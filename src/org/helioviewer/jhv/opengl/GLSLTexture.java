package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.log.Log;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;

public class GLSLTexture {

    private int[] vboAttribRefs;
    private final int[] vboAttribLens = { 3, 2 };

    private final VBO[] vbos = new VBO[2];
    private VBO ivbo;
    private boolean hasPoints = false;
    private boolean inited = false;

    public void setData(GL2 gl, FloatBuffer position, FloatBuffer coords) {
        hasPoints = false;
        int plen = position.limit() / 3;
        if (position.limit() != 12 || coords.limit() != 8) {
            Log.error("Something is wrong with the vertices or coords from this GLSLTexture");
            return;
        }
        vbos[0].bindBufferData(gl, position, Buffers.SIZEOF_FLOAT);
        vbos[1].bindBufferData(gl, coords, Buffers.SIZEOF_FLOAT);

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

    public void render(GL2 gl, float[] color) {
        if (!hasPoints)
            return;

        GLSLTextureShader.texture.bind(gl);
        GLSLTextureShader.texture.setColor(color);
        GLSLTextureShader.texture.bindParams(gl);

        bindVBOs(gl);
        gl.glDrawElements(GL2.GL_TRIANGLE_FAN, ivbo.bufferSize, GL2.GL_UNSIGNED_INT, 0);
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
        if (!inited) {
            vboAttribRefs = new int[] { GLSLTextureShader.positionRef, GLSLTextureShader.coordRef };
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
