package ch.fhnw.jhv.plugins.vectors.rendering;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import javax.media.opengl.GL;
import javax.vecmath.Vector3f;

import ch.fhnw.jhv.plugins.vectors.data.VectorData;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;

import com.sun.opengl.util.BufferUtil;

/**
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * 
 *         19.08.2011
 */
public class VectorVisualizationArrows extends AbstractCylinderVisualization {

    int[] vboCover = new int[2];

    LinkedList<Vector3f> normals = new LinkedList<Vector3f>();

    private FloatBuffer vertBufferCover;
    private IntBuffer indexBufferCover;

    // number of vertices used for one cover (the arrow needs two covers!)
    int verticesCov;
    // one additional index to connect the triangle span
    int indicesCov;

    /**
     * Constructor
     */
    public VectorVisualizationArrows() {
        super();

        // change the arrow scale so that the AbstractCylinderVisualization
        // makes enough space for the arrow head of the vector
        arrowScale = 0.2f;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.vectors.rendering.AbstractCylinderVisualization#
     * prepareVBO(javax.media.opengl.GL, java.util.ArrayList,
     * ch.fhnw.jhv.plugins.vectors.data.VectorField)
     */

    public void prepareVBO(GL gl, ArrayList<VectorData> vectors, VectorField field) {
        normals.clear();

        // number of vertices used for one cover (the arrow needs two covers!)
        verticesCov = verticesCyl / 2 + 1;
        // one additional index to connect the triangle span
        indicesCov = verticesCov + 1;

        vectorcount = calculateStartEndPoints(vectors, field);

        // count of floats for all the cylinders vertices of the shell and its
        // normals
        // (multiply by 3 -> size of vector | multiply by 3 -> for normals and
        // colors)
        // multiply by 2 sinse verticesCov is only for one cover
        vertBufferCover = FloatBuffer.allocate(verticesCov * vectorcount * 3 * 3 * 2);
        indexBufferCover = IntBuffer.allocate(indicesCov * vectorcount * 2);

        super.prepareVBOShell(gl, vectorcount);

        vertBufferCover.flip();
        indexBufferCover.flip();

        // Create the Vertex Buffer for Cover
        gl.glGenBuffers(1, vboCover, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboCover[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertBufferCover.capacity() * BufferUtil.SIZEOF_FLOAT, vertBufferCover, GL.GL_STATIC_DRAW);

        // Create the Indeces Buffer for cover
        gl.glGenBuffers(1, vboCover, 1);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vboCover[1]);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indexBufferCover.capacity() * BufferUtil.SIZEOF_INT, indexBufferCover, GL.GL_STATIC_DRAW);

        vertBufferCover.clear();
        indexBufferCover.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.vectors.rendering.AbstractCylinderVisualization#
     * prepareVBOCover(int, javax.vecmath.Vector3f, javax.vecmath.Vector3f,
     * javax.vecmath.Vector3f, float[], boolean)
     */

    public void prepareVBOCover(int cylinderIndex, Vector3f startpoint, Vector3f endpoint, Vector3f direction, float[] verticesShell, boolean isOutgoing) {

        // size of arrays for one vector
        float verticesCover[] = new float[verticesCov * 3 * 3];
        int indicesCover[] = new int[indicesCov];

        /*
         * CREATE THE COVER AT STARTPOINT
         */
        // set vertex in the middle of triangle fan
        verticesCover[0] = startpoint.x;
        verticesCover[1] = startpoint.y;
        verticesCover[2] = startpoint.z;

        // move the arrow head if the vector is incoming
        if (!isOutgoing) {
            verticesCover[0] -= direction.x * arrowScale;
            verticesCover[1] -= direction.y * arrowScale;
            verticesCover[2] -= direction.z * arrowScale;
        }

        // set normal
        verticesCover[3] = -direction.x;
        verticesCover[4] = -direction.x;
        verticesCover[5] = -direction.x;

        // get the color of the 2nd vertex
        // because it is the first vertex in the triangle strip
        // which is next to startpoint
        verticesCover[6] = verticesShell[6 + 9];
        verticesCover[7] = verticesShell[7 + 9];
        verticesCover[8] = verticesShell[8 + 9];

        /*
         * get the vertices of the shell around start point get them in reverse
         * order so that the triangle fan is correctly orientated
         */
        int verticesCoverIndex = 9;
        for (int j = verticesCyl - 1; j > 0; j -= 2) {
            int index = j * 9;
            verticesCover[verticesCoverIndex] = verticesShell[index];
            verticesCover[verticesCoverIndex + 1] = verticesShell[index + 1];
            verticesCover[verticesCoverIndex + 2] = verticesShell[index + 2];

            if (!isOutgoing) {
                /*
                 * this calculation could be optimized by reusing previous
                 * calculated normals
                 */
                // calculate the normals for correct lighthing of the arrow
                Vector3f toTop = new Vector3f(verticesCover[0] - verticesCover[verticesCoverIndex], verticesCover[1] - verticesCover[verticesCoverIndex + 1], verticesCover[2] - verticesCover[verticesCoverIndex + 2]);

                int neighbour = j == 1 ? (verticesCyl - 1) * 9 : index - 18;
                Vector3f toPrevNeighbour = new Vector3f(verticesShell[neighbour] - verticesCover[verticesCoverIndex], verticesShell[neighbour + 1] - verticesCover[verticesCoverIndex + 1], verticesShell[neighbour + 2] - verticesCover[verticesCoverIndex + 2]);

                neighbour = j == verticesCyl - 1 ? 9 : index + 18;
                Vector3f toNextNeighbour = new Vector3f(verticesShell[neighbour] - verticesCover[verticesCoverIndex], verticesShell[neighbour + 1] - verticesCover[verticesCoverIndex + 1], verticesShell[neighbour + 2] - verticesCover[verticesCoverIndex + 2]);

                Vector3f normal = new Vector3f();
                normal.cross(toPrevNeighbour, toTop);
                Vector3f normal2 = new Vector3f();
                normal2.cross(toTop, toNextNeighbour);

                // normals.add(new Vector3f(verticesCover[verticesCoverIndex],
                // verticesCover[verticesCoverIndex+1],
                // verticesCover[verticesCoverIndex+2]));
                // normals.add(normal);

                normal.add(normal2);
                normal.scale(0.5f);
                normal.normalize();

            } else {
                verticesCover[verticesCoverIndex + 3] = -direction.x;
                verticesCover[verticesCoverIndex + 4] = -direction.x;
                verticesCover[verticesCoverIndex + 5] = -direction.x;
            }

            // use same color
            verticesCover[verticesCoverIndex + 6] = verticesCover[6];
            verticesCover[verticesCoverIndex + 7] = verticesCover[7];
            verticesCover[verticesCoverIndex + 8] = verticesCover[8];

            verticesCoverIndex += 9;
        }

        /*
         * create indices for triangle fan. the indices are: 0 - 1 - 2 - 3 ....
         * (verticesCov-1) - 1 (and the offset of curent index * vertices per
         * cover is added to each)
         */
        for (int j = 0; j < indicesCov; j++) {
            if (j >= verticesCov) {
                indicesCover[j] = indicesCover[1];
            } else {
                indicesCover[j] = cylinderIndex * verticesCov * 2 + j;
            }
        }

        vertBufferCover.put(verticesCover);
        indexBufferCover.put(indicesCover);

        /*
         * CREATE THE COVER AT ENDPOINT
         */

        // vertex in the middle of triangle fan
        verticesCover[0] = endpoint.x;
        verticesCover[1] = endpoint.y;
        verticesCover[2] = endpoint.z;

        // move the endpoint up if the vector is outgoing
        if (isOutgoing) {
            verticesCover[0] += direction.x * arrowScale;
            verticesCover[1] += direction.y * arrowScale;
            verticesCover[2] += direction.z * arrowScale;
        }

        // set the normal in direction of vector
        verticesCover[3] = direction.x;
        verticesCover[4] = direction.y;
        verticesCover[5] = direction.z;

        // get the color of the first vertex of the shell
        verticesCover[6] = verticesShell[6];
        verticesCover[7] = verticesShell[7];
        verticesCover[8] = verticesShell[8];

        // currently make the arrow white
        /*
         * if (isOutgoing) { verticesCover[6] = 0.9f; verticesCover[7] = 0.9f;
         * verticesCover[8] = 0.9f; }
         */

        // get the vertices of the shell around the enpoint
        verticesCoverIndex = 9;
        for (int j = 0; j < verticesCyl; j += 2) {
            int index = j * 9;
            verticesCover[verticesCoverIndex] = verticesShell[index];
            verticesCover[verticesCoverIndex + 1] = verticesShell[index + 1];
            verticesCover[verticesCoverIndex + 2] = verticesShell[index + 2];

            if (isOutgoing) {
                /*
                 * this calculation could be optimized by reusing previous
                 * calculated normals
                 */
                // calculate the normals for correct lighthing of the arrow
                Vector3f toTop = new Vector3f(verticesCover[0] - verticesCover[verticesCoverIndex], verticesCover[1] - verticesCover[verticesCoverIndex + 1], verticesCover[2] - verticesCover[verticesCoverIndex + 2]);
                int neighbour = j == 0 ? (verticesCyl - 2) * 9 : index - 18;

                Vector3f toPrevNeighbour = new Vector3f(verticesShell[neighbour] - verticesCover[verticesCoverIndex], verticesShell[neighbour + 1] - verticesCover[verticesCoverIndex + 1], verticesShell[neighbour + 2] - verticesCover[verticesCoverIndex + 2]);
                neighbour = j == verticesCyl - 2 ? 0 : index + 18;

                Vector3f toNextNeighbour = new Vector3f(verticesShell[neighbour] - verticesCover[verticesCoverIndex], verticesShell[neighbour + 1] - verticesCover[verticesCoverIndex + 1], verticesShell[neighbour + 2] - verticesCover[verticesCoverIndex + 2]);

                Vector3f normal = new Vector3f();
                normal.cross(toTop, toPrevNeighbour);
                Vector3f normal2 = new Vector3f();
                normal2.cross(toNextNeighbour, toTop);

                normal.add(normal2);
                normal.scale(0.5f);
                normal.normalize();

                // normals.add(new Vector3f(verticesCover[verticesCoverIndex],
                // verticesCover[verticesCoverIndex+1],
                // verticesCover[verticesCoverIndex+2]));
                // normals.add(normal);

                verticesCover[verticesCoverIndex + 3] = normal.x;
                verticesCover[verticesCoverIndex + 4] = normal.y;
                verticesCover[verticesCoverIndex + 5] = normal.z;

                // use same color
                verticesCover[verticesCoverIndex + 6] = verticesShell[6];
                verticesCover[verticesCoverIndex + 7] = verticesShell[7];
                verticesCover[verticesCoverIndex + 8] = verticesShell[8];

            } else {
                verticesCover[verticesCoverIndex + 3] = direction.x;
                verticesCover[verticesCoverIndex + 4] = direction.y;
                verticesCover[verticesCoverIndex + 5] = direction.z;
            }

            // use same color
            verticesCover[verticesCoverIndex + 6] = verticesShell[6];
            verticesCover[verticesCoverIndex + 7] = verticesShell[7];
            verticesCover[verticesCoverIndex + 8] = verticesShell[8];

            verticesCoverIndex += 9;
        }

        // create indices
        for (int j = 0; j < indicesCov; j++) {
            if (j >= verticesCov) {
                indicesCover[j] = indicesCover[1];
            } else {
                indicesCover[j] = cylinderIndex * verticesCov * 2 + verticesCov + j;
            }
        }

        vertBufferCover.put(verticesCover);
        indexBufferCover.put(indicesCover);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.vectors.rendering.AbstractCylinderVisualization#
     * renderCover(javax.media.opengl.GL)
     */

    public void renderCover(GL gl) {
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboCover[0]);
        gl.glVertexPointer(3, GL.GL_FLOAT, 9 * BufferUtil.SIZEOF_FLOAT, 0);

        gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboCover[0]);
        gl.glNormalPointer(GL.GL_FLOAT, 9 * BufferUtil.SIZEOF_FLOAT, 3 * BufferUtil.SIZEOF_FLOAT);

        // Describe to OpenGL where the color data is in the buffer
        gl.glEnableClientState(GL.GL_COLOR_ARRAY);
        gl.glBindBuffer(GL.GL_COLOR_ARRAY, vboCover[0]);
        gl.glColorPointer(3, GL.GL_FLOAT, 9 * BufferUtil.SIZEOF_FLOAT, 6 * BufferUtil.SIZEOF_FLOAT);

        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vboCover[1]);

        for (int i = 0; i < vectorcount; i++) {
            gl.glDrawElements(GL.GL_TRIANGLE_FAN, indicesCov, GL.GL_UNSIGNED_INT, i * indicesCov * 2 * BufferUtil.SIZEOF_INT);
            gl.glDrawElements(GL.GL_TRIANGLE_FAN, indicesCov, GL.GL_UNSIGNED_INT, (i * indicesCov * 2 + indicesCov) * BufferUtil.SIZEOF_INT);
        }

        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
        gl.glDisableClientState(GL.GL_COLOR_ARRAY);

        // bind with 0, so, switch back to normal pointer operation
        gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, 0);
        gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER_ARB, 0);

        // render normals
        Iterator<Vector3f> iter = normals.iterator();
        gl.glColor3f(0, 0, 0);
        gl.glBegin(GL.GL_LINES);
        while (iter.hasNext()) {
            Vector3f start = iter.next();

            if (iter.hasNext()) {
                Vector3f end = iter.next();
                gl.glVertex3f(start.x, start.y, start.z);
                gl.glVertex3f(start.x + end.x, start.y + end.y, start.z + end.z);
            }
        }
        gl.glEnd();
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
