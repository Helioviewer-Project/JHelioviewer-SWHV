package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

class VAO {

    protected final int[] attribLens;
    protected final VBO[] vbos;

    private int vaoID = -1;
    private boolean inited = false;

    VAO(int[] lens) {
        attribLens = lens;
        vbos = new VBO[lens.length];
    }

    public void init(GL2 gl) {
        if (!inited) {
            inited = true;

            int[] tmpId = new int[1];
            gl.glGenVertexArrays(1, tmpId, 0);
            vaoID = tmpId[0];

            gl.glBindVertexArray(vaoID);
            for (int i = 0; i < attribLens.length; i++) {
                vbos[i] = new VBO(gl);
                vbos[i].bind(gl);
                gl.glEnableVertexAttribArray(i);
                gl.glVertexAttribPointer(i, attribLens[i], GL2.GL_FLOAT, false, 0, 0);
            }
        }
    }

    public void dispose(GL2 gl) {
        if (inited) {
            inited = false;

            gl.glDeleteVertexArrays(1, new int[]{vaoID}, 0);
            vaoID = -1;

            for (int i = 0; i < vbos.length; i++) {
                vbos[i].delete(gl);
                vbos[i] = null;
            }
        }
    }

    protected void bindVAO(GL2 gl) {
        gl.glBindVertexArray(vaoID);
    }

}
