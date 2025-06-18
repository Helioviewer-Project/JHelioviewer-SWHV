package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL3;

class VAO1 {

    private final VAA[] vaa;
    private final int usage;

    private int vaoID = -1;
    private boolean inited;
    protected GLBO vbo;

    VAO1(boolean dynamic, VAA[] _vaa) {
        usage = dynamic ? GL3.GL_DYNAMIC_DRAW : GL3.GL_STATIC_DRAW;
        vaa = _vaa;
    }

    public void init(GL3 gl) {
        if (!inited) {
            inited = true;

            int[] tmpId = new int[1];
            gl.glGenVertexArrays(1, tmpId, 0);
            vaoID = tmpId[0];
            gl.glBindVertexArray(vaoID);

            vbo = new GLBO(gl, GL3.GL_ARRAY_BUFFER, usage);
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
