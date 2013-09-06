package ch.fhnw.jhv.plugins.vectors.rendering;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.vecmath.Vector3f;

import com.sun.opengl.util.BufferUtil;

import ch.fhnw.jhv.plugins.vectors.data.VectorData;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;

/**
 * This is the Basic class for all Cylinder-like Vectors.
 * 
 * It provides the creation of the VBO for the cylinder Shells.
 * 
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * 
 *         11.08.2011
 */
public abstract class AbstractCylinderVisualization extends AbstractVectorVisualization {

    /**
     * Number of vertices for one cylinder must be even.
     */
    protected int verticesCyl = 12;

    /**
     * Number of indices necessary to build the cylinder shell. There are two
     * more indices necessary for connecting the start and end of the shell
     */
    protected int indicesCyl = verticesCyl + 2;

    /**
     * vbo indices for shell
     */
    int[] vboShell = new int[4];

    /**
     * current vector count
     */
    int vectorcount;

    /**
     * This is a bit of a hack. Since the ArrowCylinders need to be moved so the
     * arrow head has place, we define the arrowScale here but we set it to
     * zero.
     */
    protected float arrowScale = 0.0f;

    /**
     * radius of cylinders
     */
    protected float radius = 0.035f;
    // protected float radius = 0.8f;

    /**
     * precalculate sine and cos values
     */
    private float sincos[][] = new float[2][verticesCyl];

    /**
     * Constructor
     */
    public AbstractCylinderVisualization() {
        float theta = 0;
        float dtheta = 2.0f * (float) Math.PI / verticesCyl;

        // store precalculation of sinus and cosinus functions
        for (int i = 0; i < verticesCyl; i++) {
            sincos[0][i] = (float) Math.sin(theta);
            sincos[1][i] = (float) Math.cos(theta);
            theta += dtheta;
        }

        // make sure arrowscale is 0
        // this should only be changed by inherited classes
        arrowScale = 0.0f;
    }

    /**
     * Render the Vertex Buffer Objects (VBO's)
     * 
     * @param GL
     *            gl
     */
    public void render(GL gl) {
        renderShell(gl);
        renderCover(gl);
    }

    /**
     * Clear all old existing VBOS
     * 
     * @param GL
     *            gl
     */
    public void clearOldVBOS(GL gl) {
        clearVBOShell(gl);
        clearVBOCover(gl);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ch.fhnw.jhv.plugins.vectors.rendering.VectorVisualization#prepareVBO(
     * javax.media.opengl.GL, java.util.ArrayList,
     * ch.fhnw.jhv.plugins.vectors.data.VectorField)
     */
    public void prepareVBO(GL gl, ArrayList<VectorData> vectors, VectorField field) {
        this.vectorcount = calculateStartEndPoints(vectors, field);
        prepareVBOShell(gl, vectorcount);
    }

    /**
     * Prepare the VBO with the Vertices and Indices of the Shell
     * 
     * @param gl
     * @param vectorcount
     */
    protected void prepareVBOShell(GL gl, int vectorcount) {

        FloatBuffer vertexBufferShell;
        IntBuffer indexBufferShell;

        // count of floats for all the cylinders vertices of the shell and its
        // normals
        // (multiply by 3 -> size of vector, multiply by 3 -> for normals and
        // colors)

        vertexBufferShell = FloatBuffer.allocate(verticesCyl * vectorcount * 3 * 3);
        indexBufferShell = IntBuffer.allocate(indicesCyl * vectorcount);

        // size of arrays for one vector
        float verticesShell[] = new float[verticesCyl * 3 * 3];
        int indicesShell[] = new int[indicesCyl];
        int currentVectorCount = 0;

        // start and end point of current vector
        Vector3f start = new Vector3f();
        Vector3f end = new Vector3f();

        for (int i = 0; i < vectorcount; i++) {

            // get precalculated start- and endpoint

            int pointer = i * 3;

            start.set(startPoints[pointer], startPoints[pointer + 1], startPoints[pointer + 2]);

            end.set(endPoints[pointer], endPoints[pointer + 1], endPoints[pointer + 2]);

            // direction vector of zylinder
            Vector3f dir = new Vector3f();
            dir.sub(end, start);
            dir.normalize();

            // This is a fix for arrow visualization:
            // Since the cylinder must be translated for an incoming vector.
            // (Otherwise the arrow head points right into the sun or plane)
            // translate the vector with its direction
            // Otherwise the arrow head is inside the sun or the plane
            if (arrowScale > 0 && !isOutgoing[i]) {

                start.set(start.x + arrowScale * dir.x, start.y + arrowScale * dir.y, start.z + arrowScale * dir.z);

                end.set(end.x + arrowScale * dir.x, end.y + arrowScale * dir.y, end.z + arrowScale * dir.z);

            }

            // the vectors X and Y form the coordinate system of the plane
            // that is defined by the direction vector of the cylinder
            // first initialize x with a vector that is not the same like
            // direction vector
            Vector3f x = new Vector3f();
            Vector3f y = new Vector3f();

            // create a vector that is no the same (or opposite) direction of
            // dir
            if (dir.y < 0.99 || dir.y < -0.99)
                x.set(0.5f, 0.5f, 0);
            else if (dir.x < 0.99 || dir.x < -0.99)
                x.set(0, 0.5f, 0.5f);
            else if (dir.z < 0.99 || dir.z < -0.99)
                x.set(0.5f, 0.5f, 0);
            else {
                x.set(dir.z, -dir.x, 2.0f);
            }

            y.cross(dir, x); // Y is orthogonal to the direction vector
            x.cross(dir, y); // X is now orthogonal to direction and Y
            y.normalize();
            x.normalize();

            float radiusStart = getRadiusStart(i);
            float radiusEnd = getRadiusEnd(i);

            // now we have all the information to build a circle around
            // the start and end point.

            // we now create all the vertices of the shell
            for (int j = 0; j < verticesCyl; j++) {
                int index = 9 * j;

                Vector3f xRatio = new Vector3f(x.x, x.y, x.z);
                Vector3f yRatio = new Vector3f(y.x, y.y, y.z);

                float thetasin = sincos[0][j];
                float thetacos = sincos[1][j];

                xRatio.scale(thetasin);
                yRatio.scale(thetacos);

                xRatio.add(yRatio);
                // xRatio now points on a circle around the direction vector

                Vector3f vertex = new Vector3f();

                if (j % 2 == 0) {
                    // end vertex
                    // specify color
                    if (isOutgoing[i]) {
                        verticesShell[index + 6] = 0.1f;
                        verticesShell[index + 7] = 0.9f;
                        verticesShell[index + 8] = 0.1f;
                    } else {
                        verticesShell[index + 6] = 0.2f;
                        verticesShell[index + 7] = 0.1f;
                        verticesShell[index + 8] = 0.1f;
                    }

                    // scale by radius and place it next to endpoint
                    xRatio.scale(radiusEnd);
                    vertex.add(end, xRatio);

                } else {
                    // start vertex
                    // set the color
                    if (isOutgoing[i]) {
                        verticesShell[index + 6] = 0.1f;
                        verticesShell[index + 7] = 0.2f;
                        verticesShell[index + 8] = 0.1f;
                    } else {
                        verticesShell[index + 6] = 0.9f;
                        verticesShell[index + 7] = 0.1f;
                        verticesShell[index + 8] = 0.1f;
                    }

                    // scale by radius and place it next to startpoint
                    xRatio.scale(radiusStart);
                    vertex.add(start, xRatio);
                }

                // vertex
                verticesShell[index] = vertex.x;
                verticesShell[index + 1] = vertex.y;
                verticesShell[index + 2] = vertex.z;

                // normal
                verticesShell[index + 3] = xRatio.x;
                verticesShell[index + 4] = xRatio.y;
                verticesShell[index + 5] = xRatio.z;
            }

            // call abstract method, so an implementation can form the cover of
            // the cylinder
            prepareVBOCover(currentVectorCount, start, end, dir, verticesShell, isOutgoing[currentVectorCount]);

            // create the indices that create the triangle

            for (int j = 0; j < indicesCyl; j++) {
                indicesShell[j] = (currentVectorCount) * verticesCyl + (j % verticesCyl);
            }

            currentVectorCount++;

            indexBufferShell.put(indicesShell);
            vertexBufferShell.put(verticesShell);
        }

        indexBufferShell.flip();
        vertexBufferShell.flip();

        // Create the Vertex Buffer for Shell
        gl.glGenBuffers(1, vboShell, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboShell[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertexBufferShell.capacity() * BufferUtil.SIZEOF_FLOAT, vertexBufferShell, GL.GL_STATIC_DRAW);

        // Create the Indeces Buffer for shell
        gl.glGenBuffers(1, vboShell, 1);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vboShell[1]);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indexBufferShell.capacity() * BufferUtil.SIZEOF_INT, indexBufferShell, GL.GL_STATIC_DRAW);

        // clear all buffers
        vertexBufferShell.clear();
        indexBufferShell.clear();

        vertexBufferShell = null;
        indexBufferShell = null;
    }

    /**
     * Render the Cylinder shell
     * 
     * @param gl
     */
    protected void renderShell(GL gl) {
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboShell[0]);
        gl.glVertexPointer(3, GL.GL_FLOAT, 9 * BufferUtil.SIZEOF_FLOAT, 0);

        gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboShell[0]);
        gl.glNormalPointer(GL.GL_FLOAT, 9 * BufferUtil.SIZEOF_FLOAT, 3 * BufferUtil.SIZEOF_FLOAT);

        // Describe to OpenGL where the color data is in the buffer
        gl.glEnableClientState(GL.GL_COLOR_ARRAY);
        gl.glBindBuffer(GL.GL_COLOR_ARRAY, vboShell[0]);
        gl.glColorPointer(3, GL.GL_FLOAT, 9 * BufferUtil.SIZEOF_FLOAT, 6 * BufferUtil.SIZEOF_FLOAT);

        // draw all the shells
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vboShell[1]);
        for (int i = 0; i < vectorcount; i++) {
            gl.glDrawElements(GL.GL_TRIANGLE_STRIP, indicesCyl, GL.GL_UNSIGNED_INT, i * indicesCyl * BufferUtil.SIZEOF_INT);
        }

        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
        gl.glDisableClientState(GL.GL_COLOR_ARRAY);

        // bind with 0, so, switch back to normal pointer operation
        gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, 0);
        gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER_ARB, 0);
    }

    /**
     * Clear VBO of the shell
     * 
     * @param gl
     */
    protected void clearVBOShell(GL gl) {
        gl.glDeleteBuffers(1, vboShell, 0);
        gl.glDeleteBuffers(1, vboShell, 1);
    }

    /**
     * Get the radius at the start point of the vector. Note that the startpoint
     * is independet of the vectors orientation, the point that is on the sun,
     * or on the plane is meant.
     * 
     * @param cylinderIndex
     *            Index of Cylinder
     * @return
     */
    public float getRadiusStart(int cylinderIndex) {
        return radius;
    }

    /**
     * Get the radius at the end point of the vector. Note that the endpoint is
     * independet of the vectors orientation, the point that is NOT on the sun
     * and NOT on the plane is meant.
     * 
     * @param cylinderIndex
     *            Index of Cylinder
     * @return
     */
    public float getRadiusEnd(int cylinderIndex) {
        return radius;
    }

    /**
     * Create the VBO for the Cover for one Cylinder. Since this class only
     * provides the rendering of the cylinder' shell, all class that inherit
     * this class need to implement an implementation of the covers
     * 
     * @param cylinderIndex
     *            Index of Cylinder
     * @param startpoint
     *            Start point of vector
     * @param endpoint
     *            End point of vector
     * @param direction
     *            Direction is supplied that it doesn't need to be recalculated
     * @param verticesShell
     *            The vertices of the shell can be reused
     * @param isOutgoing
     *            True if the vector is outgoing
     */
    protected abstract void prepareVBOCover(int cylinderIndex, Vector3f startpoint, Vector3f endpoint, Vector3f direction, float[] verticesShell, boolean isOutgoing);

    /**
     * Render the Cover of the Cylinder
     * 
     * @param gl
     */
    protected abstract void renderCover(GL gl);

    /**
     * Clear Covers of VBO
     * 
     * @param gl
     */
    protected abstract void clearVBOCover(GL gl);

}
