package org.helioviewer.jhv.opengl;

import org.lwjgl.opengl.GL33;

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
        type = _normalized ? GL33.GL_UNSIGNED_BYTE : GL33.GL_FLOAT;
        normalized = _normalized;
        stride = _stride;
        offset = _offset;
        divisor = _divisor;
    }

    void enable() {
        GL33.glEnableVertexAttribArray(index);
        GL33.glVertexAttribPointer(index, size, type, normalized, stride, offset);
        GL33.glVertexAttribDivisor(index, divisor);
    }

}
