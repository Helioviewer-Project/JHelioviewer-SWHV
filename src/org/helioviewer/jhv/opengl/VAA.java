package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

class VAA {

    private final int index;
    private final int size;
    private final int type;
    private final boolean normalized;
    private final long offset;
    private final int divisor;

    VAA(int _index, int _size, boolean _normalized, long _offset, int _divisor) {
        index = _index;
        size = _size;
        type = _normalized ? GL2.GL_UNSIGNED_BYTE : GL2.GL_FLOAT;
        normalized = _normalized;
        offset = _offset;
        divisor = _divisor;
    }

    void enable(GL2 gl) {
        gl.glEnableVertexAttribArray(index);
        gl.glVertexAttribPointer(index, size, type, normalized, 0, offset);
        gl.glVertexAttribDivisor(index, divisor);
    }

}
