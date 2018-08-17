package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

class VAO {

    protected final VBO[] vbo;
    private final VAA[] vaa;

    private int vaoID = -1;
    private boolean inited = false;

    VAO(int nvbo, VAA[] _vaa) {
        vbo = new VBO[nvbo];
        vaa = _vaa;
    }

    public void init(GL2 gl) {
        if (!inited) {
            inited = true;

            int[] tmpId = new int[1];
            gl.glGenVertexArrays(1, tmpId, 0);
            vaoID = tmpId[0];

            for (int i = 0; i < vbo.length; i++) {
                vbo[i] = new VBO(gl);
            }

            gl.glBindVertexArray(vaoID);
            for (int i = 0; i < vaa.length; i++) {
                vbo[i % vbo.length].bind(gl);
                vaa[i].enable(gl);
            }
        }
    }

    public void dispose(GL2 gl) {
        if (inited) {
            inited = false;

            gl.glDeleteVertexArrays(1, new int[]{vaoID}, 0);
            vaoID = -1;

            for (int i = 0; i < vbo.length; i++) {
                vbo[i].delete(gl);
                vbo[i] = null;
            }
        }
    }

    protected void bind(GL2 gl) {
        gl.glBindVertexArray(vaoID);
    }

}
