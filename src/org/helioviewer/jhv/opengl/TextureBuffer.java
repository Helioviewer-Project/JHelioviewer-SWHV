package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import com.jogamp.opengl.GL3;

class TextureBuffer {

    private final int bufferID;
    private final int texBufferID;

    TextureBuffer(GL3 gl, Buffer buf) {
        int[] tmpId = new int[1];
        // Generate and bind buffer
        gl.glGenBuffers(1, tmpId, 0);
        bufferID = tmpId[0];
        gl.glBindBuffer(GL3.GL_TEXTURE_BUFFER, bufferID);
        gl.glBufferData(GL3.GL_TEXTURE_BUFFER, buf.capacity(), buf, GL3.GL_STATIC_DRAW);
        // Generate and bind texture buffer
        gl.glGenTextures(1, tmpId, 0);
        texBufferID = tmpId[0];
        gl.glBindTexture(GL3.GL_TEXTURE_BUFFER, texBufferID);
        gl.glTexBuffer(GL3.GL_TEXTURE_BUFFER, GL3.GL_RGBA8, bufferID);
    }

    void delete(GL3 gl) {
        gl.glDeleteBuffers(1, new int[]{bufferID}, 0);
        gl.glDeleteTextures(1, new int[]{texBufferID}, 0);
    }

    void bind(GL3 gl) {
        gl.glBindTexture(GL3.GL_TEXTURE_BUFFER, texBufferID);
    }

    // uniform samplerBuffer colorMapTex;

}
