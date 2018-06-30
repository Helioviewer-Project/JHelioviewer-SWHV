package org.helioviewer.jhv.opengl;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;

import org.helioviewer.jhv.math.IcoSphere;

import com.jogamp.opengl.GL2;

public class GLSLSolar {

    private static final FloatBuffer positionBuffer = IcoSphere.IcoSphere.a;
    private static final IntBuffer indexBuffer = IcoSphere.IcoSphere.b;
    private static final int indexSize = indexBuffer.limit();

    private static final VBO positionVBO = VBO.gen_float_VBO(0, 4);
    private static final VBO indexVBO = VBO.gen_index_VBO();

    public static void renderDisc(GL2 gl) {
        bindVBOs(gl);
        gl.glDrawElements(GL2.GL_TRIANGLES, indexSize - 6, GL2.GL_UNSIGNED_INT, 0);
        unbindVBOs(gl);
    }

    public static void render(GL2 gl) {
        bindVBOs(gl);
        gl.glDrawElements(GL2.GL_TRIANGLES, 6, GL2.GL_UNSIGNED_INT, 4 * (indexSize - 6));
        unbindVBOs(gl);
    }

    private static void bindVBOs(GL2 gl) {
        positionVBO.bindArray(gl);
        indexVBO.bindArray(gl);
    }

    private static void unbindVBOs(GL2 gl) {
        positionVBO.unbindArray(gl);
        indexVBO.unbindArray(gl);
    }

    static void init(GL2 gl) {
        positionVBO.init(gl);
        positionVBO.bindBufferData4(gl, positionBuffer);
        indexVBO.init(gl);
        indexVBO.bindBufferData4(gl, indexBuffer);
    }

    static void dispose(GL2 gl) {
        positionVBO.dispose(gl);
        indexVBO.dispose(gl);
    }

}
