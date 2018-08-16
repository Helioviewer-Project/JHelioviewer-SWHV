package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLSLTexture extends VTAO {

    private int count;

    public GLSLTexture() {
        super(GL2.GL_TEXTURE0 + GLTexture.Unit.FOUR.ordinal(), new int[]{4, 2});
    }

    public void setData(GL2 gl, FloatBuffer position, FloatBuffer coord) {
        int plen = position.limit() / attribLens[0];
        if (plen * attribLens[0] != position.limit() || plen != coord.limit() / attribLens[1]) {
            Log.error("Something is wrong with the attributes of this GLSLTexture");
            return;
        }
        vtbos[0].setData4(gl, position);
        vtbos[1].setData4(gl, coord);
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
