package org.helioviewer.jhv.opengl;

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
        type = _normalized ? GL.UNSIGNED_BYTE : GL.FLOAT;
        normalized = _normalized;
        stride = _stride;
        offset = _offset;
        divisor = _divisor;
    }

    void enable() {
        GL.glEnableVertexAttribArray(index);
        GL.glVertexAttribPointer(index, size, type, normalized, stride, offset);
        GL.glVertexAttribDivisor(index, divisor);
    }

}
