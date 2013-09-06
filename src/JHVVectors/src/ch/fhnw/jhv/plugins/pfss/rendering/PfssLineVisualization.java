package ch.fhnw.jhv.plugins.pfss.rendering;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.List;

import javax.media.opengl.GL;
import javax.vecmath.Vector3f;

import ch.fhnw.jhv.plugins.pfss.data.PfssCurve;
import ch.fhnw.jhv.plugins.pfss.data.PfssDimension;

import com.sun.opengl.util.BufferUtil;

/**
 * Implementation of PfssVisualization that renders the fieldlines as simple
 * lines.
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * 
 *         10.08.2011
 */
public class PfssLineVisualization implements PfssVisualization {

    /**
     * store indices of VBOs
     */
    private int[] vbo;

    /**
     * store the currently rendered PfssDimension
     */
    PfssDimension currentDimension;

    /**
     * number of field lines that are rendered
     */
    private int curveCount;

    /**
     * number of segments per line
     */
    private int[] segmentsPerCurve;

    /**
     * total number of points (of all the fieldlines)
     */
    private int totalPoints = 0;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ch.fhnw.jhv.plugins.pfss.rendering.PfssVisualization#render(javax.media
     * .opengl.GL)
     */
    public void render(GL gl) {
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
        gl.glVertexPointer(3, GL.GL_FLOAT, 6 * BufferUtil.SIZEOF_FLOAT, 0);

        // Describe to OpenGL where the color data is in the buffer
        gl.glEnableClientState(GL.GL_COLOR_ARRAY);
        gl.glBindBuffer(GL.GL_COLOR_ARRAY, vbo[0]);
        gl.glColorPointer(3, GL.GL_FLOAT, 6 * BufferUtil.SIZEOF_FLOAT, 3 * BufferUtil.SIZEOF_FLOAT);

        // draw all the shells
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vbo[1]);
        gl.glColor3f(1, 1, 1);

        // draw the lines
        int currentVBOIndex = 0;
        for (int i = 0; i < segmentsPerCurve.length; i++) {
            gl.glDrawElements(GL.GL_LINE_STRIP, segmentsPerCurve[i], GL.GL_UNSIGNED_INT, currentVBOIndex * BufferUtil.SIZEOF_INT);
            currentVBOIndex += segmentsPerCurve[i];
        }

        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL.GL_COLOR_ARRAY);

        // bind with 0, so, switch back to normal pointer operation
        gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, 0);
        gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER_ARB, 0);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ch.fhnw.jhv.plugins.pfss.rendering.PfssVisualization#prepareVBO(javax
     * .media.opengl.GL, ch.fhnw.jhv.plugins.pfss.data.PfssDimension)
     */
    public void prepareVBO(GL gl, PfssDimension pfss) {
        List<PfssCurve> curves = pfss.curves;
        Iterator<PfssCurve> iterator = curves.iterator();

        totalPoints = 0;

        FloatBuffer vertBuffer;
        IntBuffer indexBuffer;

        // count points
        Iterator<PfssCurve> curveIterator = curves.iterator();
        while (curveIterator.hasNext()) {
            totalPoints += curveIterator.next().points.size();
        }

        vertBuffer = FloatBuffer.allocate(2 * 3 * totalPoints);
        indexBuffer = IntBuffer.allocate(totalPoints);

        curveCount = curves.size();
        segmentsPerCurve = new int[curveCount];
        int curveCounter = 0;
        int indexCounter = 0;

        // store all the points in the VBO
        while (iterator.hasNext()) {
            PfssCurve curve = iterator.next();
            assert (curve.points.size() > 1) : "At least two Points required to draw a curve";
            Iterator<Vector3f> iteratorPoints = curve.points.iterator();
            int pointCounter = 0;

            while (iteratorPoints.hasNext()) {

                Vector3f point = iteratorPoints.next();
                vertBuffer.put(point.x);
                vertBuffer.put(point.y);
                vertBuffer.put(point.z);

                vertBuffer.put(curve.color.x);
                vertBuffer.put(curve.color.y);
                vertBuffer.put(curve.color.z);

                indexBuffer.put(indexCounter++);

                pointCounter++;
            }

            segmentsPerCurve[curveCounter] = pointCounter;
            curveCounter++;
        }
        indexBuffer.flip();
        vertBuffer.flip();

        vbo = new int[2];

        // Create the Vertex Buffer
        gl.glGenBuffers(1, vbo, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertBuffer.capacity() * BufferUtil.SIZEOF_FLOAT, vertBuffer, GL.GL_STATIC_DRAW);

        // Create the Indeces Buffer for shell
        gl.glGenBuffers(1, vbo, 1);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vbo[1]);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * BufferUtil.SIZEOF_INT, indexBuffer, GL.GL_STATIC_DRAW);

        indexBuffer.clear();
        vertBuffer.clear();

        indexBuffer = null;
        vertBuffer = null;
    }

    public void clearVBO(GL gl) {
        gl.glDeleteBuffers(1, vbo, 0);
        gl.glDeleteBuffers(1, vbo, 1);
    }
}
