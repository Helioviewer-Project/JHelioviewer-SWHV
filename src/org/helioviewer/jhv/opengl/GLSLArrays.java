package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

class GLSLArrays {

    protected final int[] attribLens;
    protected final VAO[] vaos;

    private boolean inited = false;

    GLSLArrays(int[] lens) {
        attribLens = lens;
        vaos = new VAO[lens.length];
    }

    public void init(GL2 gl) {
        if (!inited) {
            inited = true;
            for (int i = 0; i < attribLens.length; i++) {
                vaos[i] = new VAO(i, attribLens[i]);
                vaos[i].generate(gl);
            }
        }
    }

    public void dispose(GL2 gl) {
        if (inited) {
            inited = false;
            for (int i = 0; i < vaos.length; i++) {
                vaos[i].delete(gl);
                vaos[i] = null;
            }
        }
    }

    protected void bindVAOs(GL2 gl) {
        for (VAO vao : vaos) {
            vao.bind(gl);
        }
    }

    protected void unbindVAOs(GL2 gl) {
        for (VAO vao : vaos) {
            vao.bind(gl);
        }
    }

}
