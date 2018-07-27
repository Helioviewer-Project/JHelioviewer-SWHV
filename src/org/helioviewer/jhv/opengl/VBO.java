package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import com.jogamp.opengl.GL2;

class VBO {

    private final int bufferType;
    private final int attribRef;
    private final int numComponents;

    private int bufferID = -1;
    private int bufferLength = -1;

    private VBO(int _bufferType, int _attribRef, int _numComponents) {
        attribRef = _attribRef;
        bufferType = _bufferType;
        numComponents = _numComponents;
    }

    static VBO gen_index_VBO() {
        return new VBO(GL2.GL_ELEMENT_ARRAY_BUFFER, -1, -1);
    }

    static VBO gen_float_VBO(int _attribRef, int _numComponents) {
        return new VBO(GL2.GL_ARRAY_BUFFER, _attribRef, _numComponents);
    }

    void bindArray(GL2 gl) {
        gl.glBindBuffer(bufferType, bufferID);
        if (attribRef != -1) {
            gl.glEnableVertexAttribArray(attribRef);
            gl.glVertexAttribPointer(attribRef, numComponents, GL2.GL_FLOAT, false, 0, 0);
        }
    }

    void init(GL2 gl) {
        bufferID = generate(gl);
    }

    void dispose(GL2 gl) {
        gl.glDeleteBuffers(1, new int[]{bufferID}, 0);
        bufferID = -1;
        bufferLength = -1;
    }

    void setData4(GL2 gl, Buffer buffer) {
        gl.glBindBuffer(bufferType, bufferID);
        int length = 4 * buffer.limit();
        if (length != bufferLength) {
            bufferLength = length;
            gl.glBufferData(bufferType, length, null, GL2.GL_STATIC_DRAW);
        }
        gl.glBufferSubData(bufferType, 0, length, buffer);
    }

    private static int generate(GL2 gl) {
        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        return tmpId[0];
    }

}
