package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import com.jogamp.opengl.GL3;

class GLBO {

    private final int target;
    private final int bufferID;
    private final int usage;

    GLBO(GL3 gl, int _target, int _usage) {
        target = _target;
        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        bufferID = tmpId[0];
        usage = _usage;
    }

    void delete(GL3 gl) {
        gl.glDeleteBuffers(1, new int[]{bufferID}, 0);
    }

    void bind(GL3 gl) {
        gl.glBindBuffer(target, bufferID);
    }

    void setBufferData(GL3 gl, int limit, int capacity, Buffer buffer) {
        gl.glBindBuffer(target, bufferID);
        gl.glBufferData(target, capacity, null, usage); // orphan, https://www.khronos.org/opengl/wiki/Buffer_Object_Streaming#Buffer_re-specification
        gl.glBufferSubData(target, 0, limit, buffer);
    }

    int getID() {
        return bufferID;
    }

}
