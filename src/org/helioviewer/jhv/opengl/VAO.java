package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import com.jogamp.opengl.GL2;

class VAO {

    private final int attribRef;
    private final int attribLen;

    private int bufferID = -1;

    VAO(int _attribRef, int _attribLen) {
        attribRef = _attribRef;
        attribLen = _attribLen;
    }

    void generate(GL2 gl) {
        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        bufferID = tmpId[0];
        gl.glEnableVertexAttribArray(attribRef);
    }

    void delete(GL2 gl) {
        gl.glDeleteBuffers(1, new int[]{bufferID}, 0);
        bufferID = -1;
    }

    void bind(GL2 gl) {
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferID);
        gl.glVertexAttribPointer(attribRef, attribLen, GL2.GL_FLOAT, false, 0, 0);
    }

    void setData4(GL2 gl, Buffer buffer) {
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferID);
        int length = 4 * buffer.limit();
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, length, null, GL2.GL_STATIC_DRAW); // https://www.khronos.org/opengl/wiki/Buffer_Object_Streaming#Buffer_re-specification
        gl.glBufferSubData(GL2.GL_ARRAY_BUFFER, 0, length, buffer);
    }

}
