package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

public class GLSLTexture extends VAO1 {

    private static final int size0 = 4;
    private static final int size1 = 2;
    private static final int stride = 4 * (size0 + size1);

    private int count;

    public GLSLTexture() {
        super(true, new VAA[]{new VAA(0, size0, false, stride, 0, 0), new VAA(1, size1, false, stride, 4 * size0, 0)});
    }

    public void setCoord(BufCoord buf) {
        count = buf.getCount();
        if (count == 0)
            return;

        Buffer buffer = buf.toBuffer();
        vbo.setBufferData(4 * buffer.limit(), 4 * buffer.capacity(), buffer);
        buf.clear();
    }

    public void renderTexture(int mode, float[] color, int first, int toDraw) {
        render(mode, color, first, toDraw, GLSLTextureShader.texture);
    }

    public void renderTextTexture(int mode, float[] color, int first, int toDraw) {
        render(mode, color, first, toDraw, GLSLTextureShader.text);
    }

    public void renderMsdfTexture(int mode, float[] color, float unitRangeX, float unitRangeY, int first, int toDraw) {
        if (count == 0 || toDraw > count)
            return;

        GLSLTextureShader shader = GLSLTextureShader.msdf;
        shader.use();
        shader.bindMsdfParams(color, unitRangeX, unitRangeY);
        shader.bindMVP();

        bind();
        GL.glDrawArrays(mode, first, toDraw);
    }

    private void render(int mode, float[] color, int first, int toDraw, GLSLTextureShader shader) {
        if (count == 0 || toDraw > count)
            return;

        shader.use();
        shader.bindParams(color);
        shader.bindMVP();

        bind();
        GL.glDrawArrays(mode, first, toDraw);
    }

}
