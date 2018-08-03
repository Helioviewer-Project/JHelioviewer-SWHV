package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import com.jogamp.opengl.GL2;

class PBO {

    private int[] bufferID;

    void bind(GL2 gl, int idx) {
        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, bufferID[idx]);
    }

    void unbind(GL2 gl) {
        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
    }

    void delete(GL2 gl) {
        gl.glDeleteBuffers(bufferID.length, bufferID, 0);
    }

    void setData(GL2 gl, Buffer buffer, int elementSize) {
        int length = elementSize * buffer.limit();
        gl.glBufferData(GL2.GL_PIXEL_UNPACK_BUFFER, length, null, GL2.GL_STREAM_DRAW); // https://www.khronos.org/opengl/wiki/Buffer_Object_Streaming#Buffer_re-specification
        gl.glBufferSubData(GL2.GL_PIXEL_UNPACK_BUFFER, 0, length, buffer);
    }

    void generate(GL2 gl, int count) {
        bufferID = new int[count];
        gl.glGenBuffers(count, bufferID, 0);
    }

}
