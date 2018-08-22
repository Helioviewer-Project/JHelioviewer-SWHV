package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import org.helioviewer.jhv.base.Buf;

import com.jogamp.opengl.GL2;

class VBO {

    private final int bufferID;
    private final int usage;

    VBO(GL2 gl, boolean dynamic) {
        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        bufferID = tmpId[0];
        usage = dynamic ? GL2.GL_DYNAMIC_DRAW : GL2.GL_STATIC_DRAW;
    }

    void delete(GL2 gl) {
        gl.glDeleteBuffers(1, new int[]{bufferID}, 0);
    }

    void bind(GL2 gl) {
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferID);
    }

    void setData(GL2 gl, Buf buf) {
        Buffer buffer = buf.toBuffer();
        setBufferData(gl, buffer.limit(), buffer.capacity(), buffer);
        buf.clear();
    }

    private void setBufferData(GL2 gl, int limit, int capacity, Buffer buffer) {
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferID);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, capacity, null, usage); // https://www.khronos.org/opengl/wiki/Buffer_Object_Streaming#Buffer_re-specification
        gl.glBufferSubData(GL2.GL_ARRAY_BUFFER, 0, limit, buffer);
    }

}
