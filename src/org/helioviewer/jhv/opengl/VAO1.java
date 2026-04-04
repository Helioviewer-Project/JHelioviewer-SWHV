package org.helioviewer.jhv.opengl;

import org.lwjgl.opengl.GL33;

class VAO1 {

    private final VAA[] vaa;
    private final int usage;

    private int vaoID = -1;
    private boolean inited;
    protected GLBO vbo;

    VAO1(boolean dynamic, VAA[] _vaa) {
        usage = dynamic ? GL33.GL_DYNAMIC_DRAW : GL33.GL_STATIC_DRAW;
        vaa = _vaa;
    }

    public void init() {
        if (!inited) {
            inited = true;

            vaoID = GL33.glGenVertexArrays();
            GL33.glBindVertexArray(vaoID);

            vbo = new GLBO(GL33.GL_ARRAY_BUFFER, usage);
            vbo.bind();
            for (VAA avaa : vaa) {
                avaa.enable();
            }
        }
    }

    public void dispose() {
        if (inited) {
            inited = false;

            vbo.delete();
            GL33.glDeleteVertexArrays(vaoID);
            vaoID = -1;
        }
    }

    protected void bind() {
        GL33.glBindVertexArray(vaoID);
    }

}
