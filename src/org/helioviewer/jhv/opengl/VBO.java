package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import com.jogamp.opengl.GL2;

public class VBO {

    private int bufferID = -1;
    private int attribRef = -1;
    private final int buffer_type;
    private int vec_len = 1;
    public int bufferSize = -1;

    private VBO(int _buffer_type, int _attribRef, int _vec_len) {
        attribRef = _attribRef;
        buffer_type = _buffer_type;
        vec_len = _vec_len;
    }

    public static VBO gen_index_VBO() {
        return new VBO(GL2.GL_ELEMENT_ARRAY_BUFFER, -1, -1);
    }

    public static VBO gen_float_VBO(int _attribRef, int _vec_len) {
        return new VBO(GL2.GL_ARRAY_BUFFER, _attribRef, _vec_len);
    }

    public static VBO gen_float_no_attrib_VBO() {
        return new VBO(GL2.GL_ARRAY_BUFFER, -1, -1);
    }

    public void bindArray(GL2 gl) {
        gl.glBindBuffer(buffer_type, bufferID);
        if (attribRef != -1) {
            gl.glVertexAttribPointer(attribRef, vec_len, GL2.GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(attribRef);
        }
    }

    public void unbindArray(GL2 gl) {
        if (attribRef != -1)
            gl.glDisableVertexAttribArray(attribRef);
        gl.glBindBuffer(buffer_type, 0);
    }

    public void init(GL2 gl) {
        bufferID = generate(gl);
    }

    public void dispose(GL2 gl) {
        gl.glDeleteBuffers(1, new int[] { bufferID }, 0);
        bufferID = -1;
        bufferSize = -1;
    }

    public void bindBufferData(GL2 gl, Buffer buffer, int buf_sizeof) {
        gl.glBindBuffer(buffer_type, bufferID);
        bufferSize = buffer.limit();
        gl.glBufferData(buffer_type, bufferSize * buf_sizeof, buffer, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(buffer_type, 0);
    }

    private static int generate(GL2 gl) {
        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        return tmpId[0];
    }

}
