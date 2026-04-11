package org.helioviewer.jhv.opengl;

class VAO {

    private final VAA[] vaa;
    protected final GLBO[] vbo;
    private final int usage;

    private int vaoID = -1;
    private boolean inited;

    VAO(int nvbo, boolean dynamic, VAA[] _vaa) {
        vbo = new GLBO[nvbo];
        vaa = _vaa;
        usage = dynamic ? GL.DYNAMIC_DRAW : GL.STATIC_DRAW;
    }

    public void init() {
        if (!inited) {
            inited = true;

            vaoID = GL.glGenVertexArray();

            for (int i = 0; i < vbo.length; i++) {
                vbo[i] = new GLBO(GL.ARRAY_BUFFER, usage);
            }

            GL.glBindVertexArray(vaoID);
            for (int i = 0; i < vaa.length; i++) {
                vbo[i % vbo.length].bind(); //!
                vaa[i].enable();
            }
        }
    }

    public void dispose() {
        if (inited) {
            inited = false;
            GL.glDeleteVertexArray(vaoID);
            vaoID = -1;

            for (int i = 0; i < vbo.length; i++) {
                vbo[i].delete();
                vbo[i] = null;
            }
        }
    }

    protected void bind() {
        GL.glBindVertexArray(vaoID);
    }

}
