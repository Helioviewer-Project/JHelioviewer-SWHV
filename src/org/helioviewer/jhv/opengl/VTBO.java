package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import com.jogamp.opengl.GL2;

class VTBO {

    private final int bufferID;
    private int textureID;
    private final int target;
    private final int size;

    VTBO(GL2 gl, int _target, int _size) {
        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        bufferID = tmpId[0];
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferID);

        target = _target;
        size = mapSizeToInternalFormat(_size);
    }

    void delete(GL2 gl) {
        gl.glDeleteBuffers(1, new int[]{bufferID}, 0);
        gl.glDeleteTextures(1, new int[]{textureID}, 0);
    }

    void setData4(GL2 gl, Buffer buffer) {
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferID);
        int length = 4 * buffer.limit();
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, length, null, GL2.GL_STATIC_DRAW); // https://www.khronos.org/opengl/wiki/Buffer_Object_Streaming#Buffer_re-specification
        gl.glBufferSubData(GL2.GL_ARRAY_BUFFER, 0, length, buffer);

        gl.glDeleteTextures(1, new int[]{textureID}, 0);
        int[] tmpId = new int[1];
        gl.glGenTextures(1, tmpId, 0);
        textureID = tmpId[0];
        gl.glActiveTexture(target);
        gl.glBindTexture(GL2.GL_TEXTURE_BUFFER, textureID);
        gl.glTexBuffer(GL2.GL_TEXTURE_BUFFER, size, bufferID);
    }

    void bind(GL2 gl) {
        gl.glActiveTexture(target);
        gl.glBindTexture(GL2.GL_TEXTURE_BUFFER, textureID);
    }

    private static int mapSizeToInternalFormat(int size) {
        switch (size) {
            case 1:
                return GL2.GL_R32F;
            case 2:
                return GL2.GL_RG32F;
            case 3:
                return GL2.GL_RGB32F;
            case 4:
                return GL2.GL_RGBA32F;
            default:
                throw new IllegalArgumentException("Size is not supported");
        }
    }

}
