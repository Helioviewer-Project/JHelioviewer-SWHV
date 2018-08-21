package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

class VAO {

    private static int boundID; // track state

    private final VAA[] vaa;
    private final boolean dynamic;

    private int vaoID = -1;
    private boolean inited;
    protected VBO vbo;

    VAO(boolean _dynamic, VAA[] _vaa) {
        dynamic = _dynamic;
        vaa = _vaa;
    }

    public void init(GL2 gl) {
        if (!inited) {
            inited = true;

            int[] tmpId = new int[1];
            gl.glGenVertexArrays(1, tmpId, 0);
            vaoID = tmpId[0];

            vbo = new VBO(gl, dynamic);
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
        if (boundID != vaoID) {
            boundID = vaoID;
            gl.glBindVertexArray(vaoID);
        }
    }

}
