package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLSLTexture extends VAO {

    private static final int size0 = 4;
    private static final int size1 = 2;
    private int count;

    public GLSLTexture() {
        super(2, new VAA[]{new VAA(0, size0, false, 0, 0), new VAA(1, size1, false, 0, 0)});
    }

    public void setData(GL2 gl, FloatBuffer position, FloatBuffer coord) {
        int plen = position.limit() / size0;
        if (plen * size0 != position.limit() || plen != coord.limit() / size1) {
            Log.error("Something is wrong with the attributes of this GLSLTexture");
            return;
        }
        vbo[0].setData(gl, position, 4);
        vbo[1].setData(gl, coord, 4);
        count = plen;
    }

    public void render(GL2 gl, int mode, float[] color, int toDraw) {
        if (count == 0 || toDraw > count)
            return;

        GLSLTextureShader.texture.bind(gl);
        GLSLTextureShader.texture.setColor(color);
        GLSLTextureShader.texture.bindParams(gl);

        bind(gl);
        gl.glDrawArrays(mode, 0, toDraw);

        GLSLShader.unbind(gl);
    }

}
