package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import com.jogamp.opengl.GL2;

class VBO {

    private final int target;
    private final int bufferID;
    private final int usage;

    VBO(GL2 gl, int _target, boolean dynamic) {
        target = _target;
        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        bufferID = tmpId[0];
        usage = dynamic ? GL2.GL_DYNAMIC_DRAW : GL2.GL_STATIC_DRAW;
    }

    void delete(GL2 gl) {
        gl.glDeleteBuffers(1, new int[]{bufferID}, 0);
    }

    void bind(GL2 gl) {
        gl.glBindBuffer(target, bufferID);
    }

    void setData(GL2 gl, BufVertex buf) {
        Buffer buffer = buf.toBuffer();
        setBufferData(gl, buffer.limit(), buffer.capacity(), buffer);
        buf.clear();
    }

    void setBufferData(GL2 gl, int limit, int capacity, Buffer buffer) {
        gl.glBindBuffer(target, bufferID);
        gl.glBufferData(target, capacity, null, usage); // https://www.khronos.org/opengl/wiki/Buffer_Object_Streaming#Buffer_re-specification
        gl.glBufferSubData(target, 0, limit, buffer);
    }

}
