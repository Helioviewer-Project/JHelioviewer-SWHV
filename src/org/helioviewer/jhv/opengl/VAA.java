package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

class VAA {

    private final int index;
    private final int elements;
    private final long offset;
    private final int divisor;

    VAA(int _index, int _elements, long _offset, int _divisor) {
        index = _index;
        elements = _elements;
        offset = _offset;
        divisor = _divisor;
    }

    void enable(GL2 gl) {
        gl.glEnableVertexAttribArray(index);
        gl.glVertexAttribPointer(index, elements, GL2.GL_FLOAT, false, 0, offset);
        gl.glVertexAttribDivisor(index, divisor);
    }

}
