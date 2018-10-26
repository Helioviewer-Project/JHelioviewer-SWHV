package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

class VAO {

    private final VAA[] vaa;
    protected final VBO[] vbo;

    private final boolean dynamic;
    private int vaoID = -1;
    private boolean inited;

    VAO(int nvbo, boolean _dynamic, VAA[] _vaa) {
        vbo = new VBO[nvbo];
        dynamic = _dynamic;
        vaa = _vaa;
    }

    public void init(GL2 gl) {
        if (!inited) {
            inited = true;

            int[] tmpId = new int[1];
            gl.glGenVertexArrays(1, tmpId, 0);
            vaoID = tmpId[0];

            for (int i = 0; i < vbo.length; i++) {
                vbo[i] = new VBO(gl, GL2.GL_ARRAY_BUFFER, dynamic);
            }

            gl.glBindVertexArray(vaoID);
            for (int i = 0; i < vaa.length; i++) {
                vbo[i % vbo.length].bind(gl); //!
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
