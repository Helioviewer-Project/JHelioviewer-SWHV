package org.helioviewer.jhv.opengl;

import java.nio.Buffer;
import java.nio.FloatBuffer;

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
        bufferData(gl, buffer.limit(), buffer);
        buf.clear();
    }

    void setData(GL2 gl, FloatBuffer buffer) {
        bufferData(gl, 4 * buffer.limit(), buffer);
    }

    private void bufferData(GL2 gl, int size, Buffer buffer) {
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferID);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, size, null, usage); // https://www.khronos.org/opengl/wiki/Buffer_Object_Streaming#Buffer_re-specification
        gl.glBufferSubData(GL2.GL_ARRAY_BUFFER, 0, size, buffer);
    }

}
