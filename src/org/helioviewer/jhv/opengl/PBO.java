package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import com.jogamp.opengl.GL2;

class PBO {

    private int bufferID = -1;

    public PBO(GL2 gl) {
        bufferID = generate(gl);
    }

    void bind(GL2 gl) {
        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, bufferID);
    }

    void unbind(GL2 gl) {
        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
    }

    void dispose(GL2 gl) {
        gl.glDeleteBuffers(1, new int[]{bufferID}, 0);
        bufferID = -1;
    }

    void setData(GL2 gl, Buffer buffer, int elementSize) {
        int length = elementSize * buffer.limit();
        gl.glBufferData(GL2.GL_PIXEL_UNPACK_BUFFER, length, null, GL2.GL_STREAM_DRAW); // https://www.khronos.org/opengl/wiki/Buffer_Object_Streaming#Buffer_re-specification
        gl.glBufferSubData(GL2.GL_PIXEL_UNPACK_BUFFER, 0, length, buffer);
    }

    private static int generate(GL2 gl) {
        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        return tmpId[0];
    }

}
