package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL3;
import org.lwjgl.opengl.GL33;

class VAO {

    private final VAA[] vaa;
    protected final GLBO[] vbo;
    private final int usage;

    private int vaoID = -1;
    private boolean inited;

    VAO(int nvbo, boolean dynamic, VAA[] _vaa) {
        vbo = new GLBO[nvbo];
        vaa = _vaa;
        usage = dynamic ? GL33.GL_DYNAMIC_DRAW : GL33.GL_STATIC_DRAW;
    }

    public void init(GL3 gl) {
        if (!inited) {
            inited = true;

            vaoID = GL33.glGenVertexArrays();

            for (int i = 0; i < vbo.length; i++) {
                vbo[i] = new GLBO(GL33.GL_ARRAY_BUFFER, usage);
            }

            GL33.glBindVertexArray(vaoID);
            for (int i = 0; i < vaa.length; i++) {
                vbo[i % vbo.length].bind(); //!
                vaa[i].enable();
            }
        }
    }

    public void dispose(GL3 gl) {
        if (inited) {
            inited = false;
            GL33.glDeleteVertexArrays(vaoID);
            vaoID = -1;

            for (int i = 0; i < vbo.length; i++) {
                vbo[i].delete();
                vbo[i] = null;
            }
        }
    }

    protected void bind() {
        GL33.glBindVertexArray(vaoID);
    }

}
