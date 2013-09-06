package ch.fhnw.jhv.plugins.vectors.rendering;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.vecmath.Vector3f;

import ch.fhnw.jhv.plugins.vectors.data.VectorData;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;

import com.sun.opengl.util.BufferUtil;

/**
 * This class creates a cylinder with a flat cone
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * 
 *         19.08.2011
 */
public class VectorVisualizationCylinder extends AbstractCylinderVisualization {

    /**
     * vbo vertices
     */
    int[] vboCover = new int[2];

    /**
     * Normal Buffer for Cover
     */
    private FloatBuffer normalBufferCover;

    /**
     * Index Buffer for Cover
     */
    private IntBuffer indexBufferCover;

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.vectors.rendering.AbstractCylinderVisualization#
     * prepareVBO(javax.media.opengl.GL, java.util.ArrayList,
     * ch.fhnw.jhv.plugins.vectors.data.VectorField)
     */

    public void prepareVBO(GL gl, ArrayList<VectorData> vectors, VectorField field) {

        vectorcount = calculateStartEndPoints(vectors, field);

        // reuse the same vertex as the shell does, but with different normals
        // so we only have to create normals and indices
        normalBufferCover = FloatBuffer.allocate(verticesCyl * vectorcount * 3);
        indexBufferCover = IntBuffer.allocate(vectorcount * verticesCyl);

        // let AbstractCylinderVisualization do the job for rendering the shell.
        // inside prepareVBOShell the method prepareVBOCover of this class
        // will be called for every cylinder that is created
        super.prepareVBOShell(gl, vectorcount);

        normalBufferCover.flip();
        indexBufferCover.flip();

        // Create the Normals Buffer for Cover
        gl.glGenBuffers(1, vboCover, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboCover[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, normalBufferCover.capacity() * BufferUtil.SIZEOF_FLOAT, normalBufferCover, GL.GL_STATIC_DRAW);

        // Create the Indeces Buffer for top cover
        gl.glGenBuffers(1, vboCover, 1);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vboCover[1]);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indexBufferCover.capacity() * BufferUtil.SIZEOF_INT, indexBufferCover, GL.GL_STATIC_DRAW);

        normalBufferCover.clear();
        indexBufferCover.clear();

        normalBufferCover = null;
        indexBufferCover = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.vectors.rendering.AbstractCylinderVisualization#
     * prepareVBOCover(int, javax.vecmath.Vector3f, javax.vecmath.Vector3f,
     * javax.vecmath.Vector3f, float[], boolean)
     */

    public void prepareVBOCover(int cylinderIndex, Vector3f startpoint, Vector3f endpoint, Vector3f direction, float[] verticesShell, boolean isOutgoing) {

        // create normals
        float[] normals = new float[verticesCyl * 3];
        int[] indices = new int[verticesCyl / 2];

        int index = 0;
        for (int j = 0; j < verticesCyl; j += 2) {
            indices[index++] = (cylinderIndex) * verticesCyl + j;
            int normalIndex = j * 3;
            normals[normalIndex] = direction.x;
            normals[normalIndex] = direction.y;
            normals[normalIndex] = direction.z;
        }
        indexBufferCover.put(indices);

        index = 0;
        for (int j = verticesCyl - 1; j > 0; j -= 2) {
            indices[index++] = (cylinderIndex) * verticesCyl + j;
            int normalIndex = j * 3;
            normals[normalIndex] = -direction.x;
            normals[normalIndex] = -direction.y;
            normals[normalIndex] = -direction.z;
        }
        indexBufferCover.put(indices);
        normalBufferCover.put(normals);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.vectors.rendering.AbstractCylinderVisualization#
     * renderCover(javax.media.opengl.GL)
     */

    public void renderCover(GL gl) {
        // point to shell vertices
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboShell[0]);
        gl.glVertexPointer(3, GL.GL_FLOAT, 9 * BufferUtil.SIZEOF_FLOAT, 0);

        // load color from shell-vbo
        gl.glEnableClientState(GL.GL_COLOR_ARRAY);
        gl.glBindBuffer(GL.GL_COLOR_ARRAY, vboShell[0]);
        gl.glColorPointer(3, GL.GL_FLOAT, 9 * BufferUtil.SIZEOF_FLOAT, 6 * BufferUtil.SIZEOF_FLOAT);

        // load normals for covers
        gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboCover[0]);
        gl.glNormalPointer(GL.GL_FLOAT, 3 * BufferUtil.SIZEOF_FLOAT, 0);

        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vboCover[1]);

        int indices = verticesCyl / 2;
        for (int i = 0; i < vectorcount; i++) {
            gl.glDrawElements(GL.GL_POLYGON, indices, GL.GL_UNSIGNED_INT, i * verticesCyl * BufferUtil.SIZEOF_INT);
            gl.glDrawElements(GL.GL_POLYGON, indices, GL.GL_UNSIGNED_INT, (i * verticesCyl + indices) * BufferUtil.SIZEOF_INT);
        }

        // disable arrays
        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
        gl.glDisableClientState(GL.GL_COLOR_ARRAY);

        // bind with 0, so, switch back to normal pointer operation
        gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, 0);
        gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER_ARB, 0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.vectors.rendering.AbstractCylinderVisualization#
     * clearVBOCover(javax.media.opengl.GL)
     */

    protected void clearVBOCover(GL gl) {
        gl.glDeleteBuffers(1, vboCover, 0);
        gl.glDeleteBuffers(1, vboCover, 1);
    }

}
