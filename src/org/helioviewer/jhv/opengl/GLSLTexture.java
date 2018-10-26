package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

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

    public void setData(GL2 gl, BufCoord buf) {
        if ((count = buf.getFloats() / (size0 + size1)) == 0)
            return;
        if (count * (size0 + size1) != buf.getFloats()) {
            Log.error("Something is wrong with the attributes of this GLSLTexture");
            return;
        }

        Buffer buffer = buf.toBuffer();
        vbo.setBufferData(gl, buffer.limit(), buffer.capacity(), buffer);
        buf.clear();
    }

    public void render(GL2 gl, int mode, float[] color, int first, int toDraw) {
        if (count == 0 || toDraw > count)
            return;

        GLSLTextureShader.texture.use(gl);
        GLSLTextureShader.texture.bindParams(gl, color);
        GLSLTextureShader.texture.bindMVP(gl);

        bind(gl);
        gl.glDrawArrays(mode, first, toDraw);
    }

}
