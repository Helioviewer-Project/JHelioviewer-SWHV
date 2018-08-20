package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

class VAO {

    private final VAA[] vaa;

    private int vaoID = -1;
    private boolean inited;
    protected VBO vbo;

    VAO(VAA[] _vaa) {
        vaa = _vaa;
    }

    public void init(GL2 gl) {
        if (!inited) {
            inited = true;

            int[] tmpId = new int[1];
            gl.glGenVertexArrays(1, tmpId, 0);
            vaoID = tmpId[0];

            vbo = new VBO(gl);
            vbo.bind(gl);

            gl.glBindVertexArray(vaoID);
            for (VAA avaa : vaa) {
                avaa.enable(gl);
            }
        }
    }

    public void dispose(GL2 gl) {
        if (inited) {
            inited = false;

            vbo.delete(gl);
            gl.glDeleteVertexArrays(1, new int[]{vaoID}, 0);
            vaoID = -1;
        }
    }

    protected void bind(GL2 gl) {
        gl.glBindVertexArray(vaoID);
    }

}
