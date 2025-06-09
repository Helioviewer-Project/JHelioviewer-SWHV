package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL3;

class VAA {

    private final int index;
    private final int size;
    private final int type;
    private final boolean normalized;
    private final int stride;
    private final long offset;
    private final int divisor;

    VAA(int _index, int _size, boolean _normalized, int _stride, long _offset, int _divisor) {
        index = _index;
        size = _size;
        type = _normalized ? GL3.GL_UNSIGNED_BYTE : GL3.GL_FLOAT;
        normalized = _normalized;
        stride = _stride;
        offset = _offset;
        divisor = _divisor;
    }

    void enable(GL3 gl) {
        gl.glEnableVertexAttribArray(index);
        gl.glVertexAttribPointer(index, size, type, normalized, stride, offset);
        gl.glVertexAttribDivisor(index, divisor);
    }

}
