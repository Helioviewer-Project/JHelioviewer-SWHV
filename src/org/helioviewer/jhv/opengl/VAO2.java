package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

class VAO2 {

    private final VAA2[] vaa;

    private int vaoID = -1;
    private boolean inited;
    protected VBO2 vbo;

    VAO2(VAA2[] _vaa) {
        vaa = _vaa;
    }

    public void init(GL2 gl) {
        if (!inited) {
            inited = true;

            int[] tmpId = new int[1];
            gl.glGenVertexArrays(1, tmpId, 0);
            vaoID = tmpId[0];

            vbo = new VBO2(gl);
            vbo.bind(gl);

            gl.glBindVertexArray(vaoID);
            for (int i = 0; i < vaa.length; i++) {
                vaa[i].enable(gl);
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
