package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLSLTexture {

    private final int[] vboAttribLens = { 4, 2 };
    private final VBO[] vbos = new VBO[2];

    private int count;
    private boolean inited = false;

    public void setData(GL2 gl, FloatBuffer position, FloatBuffer coords) {
        count = 0;
        int plen = position.limit() / vboAttribLens[0];
        if (plen * vboAttribLens[0] != position.limit() || plen != coords.limit() / vboAttribLens[1]) {
            Log.error("Something is wrong with the vertices or coords from this GLSLTexture");
            return;
        }
        vbos[0].setData4(gl, position);
        vbos[1].setData4(gl, coords);
        count = plen;
    }

    public void render(GL2 gl, int mode, float[] color, int toDraw) {
        if (count == 0 || toDraw > count)
            return;

        GLSLTextureShader.texture.bind(gl);
        GLSLTextureShader.texture.setColor(color);
        GLSLTextureShader.texture.bindParams(gl);

        bindVBOs(gl);
        gl.glDrawArrays(mode, 0, toDraw);

        GLSLShader.unbind(gl);
    }

    private void bindVBOs(GL2 gl) {
        for (VBO vbo : vbos) {
            vbo.bindArray(gl);
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
