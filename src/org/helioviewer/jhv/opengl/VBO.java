package org.helioviewer.jhv.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.Buf;

import com.jogamp.opengl.GL2;

class VBO {

    private final int bufferID;

    VBO(GL2 gl) {
        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        bufferID = tmpId[0];
    }

    void delete(GL2 gl) {
        gl.glDeleteBuffers(1, new int[]{bufferID}, 0);
    }

    void bind(GL2 gl) {
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferID);
    }

    void setData(GL2 gl, Buf buf) {
        ByteBuffer buffer = buf.toBuffer();
        bufferData(gl, buffer.limit(), GL2.GL_STATIC_DRAW, buffer);
        buf.rewind();
    }

    void setData(GL2 gl, FloatBuffer buf) {
        bufferData(gl, 4 * buf.limit(), GL2.GL_STATIC_DRAW, buf);
    }

    private void bufferData(GL2 gl, int size, int usage, Buffer buffer) {
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferID);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, size, null, usage); // https://www.khronos.org/opengl/wiki/Buffer_Object_Streaming#Buffer_re-specification
        gl.glBufferSubData(GL2.GL_ARRAY_BUFFER, 0, size, buffer);
    }

}
