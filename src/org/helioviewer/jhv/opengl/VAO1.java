package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL3;

class VAO1 {

    private final VAA[] vaa;
    private final boolean dynamic;

    private int vaoID = -1;
    private boolean inited;
    protected VBO vbo;

    VAO1(boolean _dynamic, VAA[] _vaa) {
        dynamic = _dynamic;
        vaa = _vaa;
    }

    public void init(GL3 gl) {
        if (!inited) {
            inited = true;

            int[] tmpId = new int[1];
            gl.glGenVertexArrays(1, tmpId, 0);
            vaoID = tmpId[0];
            gl.glBindVertexArray(vaoID);

            vbo = new VBO(gl, GL3.GL_ARRAY_BUFFER, dynamic);
            vbo.bind(gl);
            for (VAA avaa : vaa) {
                avaa.enable(gl);
            }
        }
    }

    public void dispose(GL3 gl) {
        if (inited) {
            inited = false;

            vbo.delete(gl);
            gl.glDeleteVertexArrays(1, new int[]{vaoID}, 0);
            vaoID = -1;
        }
    }

    protected void bind(GL3 gl) {
        gl.glBindVertexArray(vaoID);
    }

}
