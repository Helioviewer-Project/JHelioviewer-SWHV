package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLSLTexture extends VAO {

    private final int attribLens0 = 4;
    private final int attribLens1 = 2;
    private int count;

    public GLSLTexture() {
        super(2, new VAA[]{new VAA(0, 4, 0, 0), new VAA(1, 2, 0, 0)});
    }

    public void setData(GL2 gl, FloatBuffer position, FloatBuffer coord) {
        int plen = position.limit() / attribLens0;
        if (plen * attribLens0 != position.limit() || plen != coord.limit() / attribLens1) {
            Log.error("Something is wrong with the attributes of this GLSLTexture");
            return;
        }
        vbo[0].setData4(gl, position);
        vbo[1].setData4(gl, coord);
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
