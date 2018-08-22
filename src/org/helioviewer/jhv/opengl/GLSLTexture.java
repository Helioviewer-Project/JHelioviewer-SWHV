package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.Buf;
import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLSLTexture extends VAO {

    private static final int size0 = 4;
    private static final int size1 = 2;
    public static final int stride = 4 * (size0 + size1);

    private int count;

    public GLSLTexture() {
        super(true, new VAA[]{new VAA(0, size0, false, stride, 0, 0), new VAA(1, size1, false, stride, 4 * size0, 0)});
    }

    public void setData(GL2 gl, Buf buf) {
        int plen = buf.getFloats() / (size0 + size1);
        if (plen * (size0 + size1) != buf.getFloats()) {
            Log.error("Something is wrong with the attributes of this GLSLTexture");
            return;
        }
        vbo.setData(gl, buf);
        count = plen;
    }

    public void render(GL2 gl, int mode, float[] color, int first, int toDraw) {
        if (count == 0 || toDraw > count)
            return;

        GLSLTextureShader.texture.bind(gl);
        GLSLTextureShader.texture.setColor(color);
        GLSLTextureShader.texture.bindParams(gl);

        bind(gl);
        gl.glDrawArrays(mode, first, toDraw);
    }

}
