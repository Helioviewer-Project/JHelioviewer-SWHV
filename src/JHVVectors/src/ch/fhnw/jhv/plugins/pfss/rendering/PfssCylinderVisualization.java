package ch.fhnw.jhv.plugins.pfss.rendering;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.List;

import javax.media.opengl.GL;
import javax.vecmath.Vector3f;

import com.sun.opengl.util.BufferUtil;

import ch.fhnw.jhv.plugins.pfss.data.PfssCurve;
import ch.fhnw.jhv.plugins.pfss.data.PfssDimension;

/**
 * Implementation of PfssVisualition that renders the lines of the PFSS model as
 * curved cylinders.
 * 
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * 
 *         12.08.2011
 */
public class PfssCylinderVisualization implements PfssVisualization {

    /**
     * number of vertices used per point of a pfss-line
     */
    int vertices = 10;

    /**
     * number of indices used to connect two points with a triangle strip
     */
    int indices = (vertices * 2) + 2;

    /**
     * radius of the curved cylinder
     */
    float radius = 0.08f;

    /**
     * pre-calculated sine and cosine values
     */
    private float sincos[][] = new float[2][vertices];

    /**
     * this array storec a refernece to the color vectors of the currently
     * rendered lines
     */
    private Vector3f[] color;

    /**
     * count of lines that are rendered
     */
    int curveCount = 0;

    /**
     * number of segments per curve. a segment means twoi points that must be
     * connected with a triangle strip. each curve has numberOfPoints - 1
     * segments. this information is necessary when the pfss lines are rendered.
     */
    int[] segmentsPerCurve;

    /**
     * store the indices of vbo's
     */
    int[] vbo;

    /**
     * Constructor
     */
    public PfssCylinderVisualization() {
        float theta = 0;
        float dtheta = 2.0f * (float) Math.PI / vertices;

        // store precalculation of sinus and cosinus functions
        for (int i = 0; i < vertices; i++) {
            sincos[0][i] = (float) Math.sin(theta);
            sincos[1][i] = (float) Math.cos(theta);
            theta += dtheta;
        }

        System.out.println("created cylinder renderer");
    }

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

        gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
        gl.glNormalPointer(GL.GL_FLOAT, 6 * BufferUtil.SIZEOF_FLOAT, 3 * BufferUtil.SIZEOF_FLOAT);

        // draw all the shells
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vbo[1]);
        int vboIndex = 0;
        for (int i = 0; i < curveCount; i++) {
            gl.glColor3f(color[i].x, color[i].y, color[i].z);
            for (int j = 0; j < segmentsPerCurve[i]; j++) {
                gl.glDrawElements(GL.GL_TRIANGLE_STRIP, indices, GL.GL_UNSIGNED_INT, vboIndex * BufferUtil.SIZEOF_INT);
                vboIndex += indices;
            }
        }

        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
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

        // total points of all lines
        int numberOfPoints = 0;

        Iterator<PfssCurve> iterator = curves.iterator();

        // count all points
        while (iterator.hasNext()) {
            PfssCurve curve = iterator.next();
            numberOfPoints += curve.points.size();
        }

        // total segments of all lines
        int numberOfSegments = numberOfPoints - pfss.curves.size();

        FloatBuffer verticesBuffer = FloatBuffer.allocate(numberOfPoints * vertices * 2 * 3);
        IntBuffer indicesBuffer = IntBuffer.allocate(numberOfSegments * indices);

        curveCount = curves.size();
        segmentsPerCurve = new int[curveCount];
        color = new Vector3f[curveCount];

        // values of vertices and normals
        float[] vertexData = new float[vertices * 3 * 2];

        int curveCounter = 0;
        int pointCounter = 0;

        iterator = curves.iterator();
        while (iterator.hasNext()) {
            PfssCurve curve = iterator.next();

            Vector3f currentPoint;
            Vector3f nextPoint;

            Vector3f xCurrent = null, yCurrent = null;

            Vector3f directionCurrent = new Vector3f();

            assert (curve.points.size() > 1) : "At least two Points necessary to draw a curve";

            Iterator<Vector3f> iteratorPoints = curve.points.iterator();

            boolean repeat = true;
            currentPoint = iteratorPoints.next();
            color[curveCounter] = curve.color;
            segmentsPerCurve[curveCounter] = curve.points.size() - 1;
            curveCounter++;

            while (repeat) {

                // create connection from currentPoint to nextPoint
                // this is done with a triangle strip
                // first we initialize the coordinate system on the plane

                if (xCurrent == null) {
                    // for the first point we have to calculate the Coordinate
                    // System
                    // on the plane of the cylinders base
                    xCurrent = new Vector3f();
                    yCurrent = new Vector3f();
                    nextPoint = iteratorPoints.next();
                    directionCurrent.sub(nextPoint, currentPoint);
                    directionCurrent.normalize();
                    calculateAxisOnPlane(directionCurrent, xCurrent, yCurrent, null);
                    createVerticeCircle(currentPoint, xCurrent, yCurrent, vertexData);
                    verticesBuffer.put(vertexData);
                    // we dont create any indices so far
                } else {
                    Vector3f oldX = new Vector3f(xCurrent);
                    if (iteratorPoints.hasNext()) {
                        nextPoint = iteratorPoints.next();
                        directionCurrent.sub(nextPoint, currentPoint);
                        directionCurrent.normalize();
                        calculateAxisOnPlane(directionCurrent, xCurrent, yCurrent, oldX);
                    } else {
                        nextPoint = null;
                    }

                    createVerticeCircle(currentPoint, xCurrent, yCurrent, vertexData);
                    verticesBuffer.put(vertexData);

                    for (int i = 0; i < vertices; i++) {
                        indicesBuffer.put(pointCounter * vertices + i);
                        indicesBuffer.put((pointCounter - 1) * vertices + i);
                    }

                    indicesBuffer.put(pointCounter * vertices);
                    indicesBuffer.put((pointCounter - 1) * vertices);
                }

                pointCounter++;

                if (nextPoint != null) {
                    currentPoint = nextPoint;
                    repeat = true;
                } else {
                    repeat = false;
                }
            }
        }
        indicesBuffer.flip();
        verticesBuffer.flip();

        vbo = new int[2];
        // Create the Vertex Buffer
        gl.glGenBuffers(1, vbo, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, verticesBuffer.capacity() * BufferUtil.SIZEOF_FLOAT, verticesBuffer, GL.GL_STATIC_DRAW);

        // Create the Indeces Buffer for shell
        gl.glGenBuffers(1, vbo, 1);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vbo[1]);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.capacity() * BufferUtil.SIZEOF_INT, indicesBuffer, GL.GL_STATIC_DRAW);

        verticesBuffer.clear();
        indicesBuffer.clear();
        verticesBuffer = null;
        indicesBuffer = null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ch.fhnw.jhv.plugins.pfss.rendering.PfssVisualization#clearVBO(javax.media
     * .opengl.GL)
     */
    public void clearVBO(GL gl) {
        gl.glDeleteBuffers(1, vbo, 0);
        gl.glDeleteBuffers(1, vbo, 1);
    }

    /**
     * Calculates two vectors that are perpendicular to each other and
     * perpendicular to direction. These two vectors are then stored in x and y,
     * which are passed as parameters.
     * 
     * @param direction
     *            Direction vector
     * @param x
     *            This vector will be set to a vector that is perpendicular to
     *            direction and y
     * @param y
     *            This vector will be set to a vector that is perpendicular to
     *            direction and x
     * @param old
     *            If this parameter is specified, then y is set to
     *            cross(direction, oldX)
     * 
     */
    private void calculateAxisOnPlane(Vector3f direction, Vector3f x, Vector3f y, Vector3f oldX) {
        if (oldX == null) {
            if (direction.y < 0.99 && direction.y > -0.99)
                x.set(-direction.x, direction.y, 0);
            else
                x.set(1, 0, 0); // if direction is straigth up, it wont work

            y.cross(direction, x); // Y is orthogonal to the direction vector
            x.cross(direction, y); // X is now orthogonal to direction and Y
            y.normalize();
            x.normalize();
        } else {
            y.cross(oldX, direction);
            y.normalize();
            x.cross(direction, y);
            x.normalize(); // maybe not necessary, since the other 2 are
                           // allready normalized
        }
    }

    /**
     * Creates the vertices on the circle around the specified point
     * 
     * @param point
     *            Center point of Circle
     * @param x
     *            X-Axis, must be perpendicular to point and y
     * @param y
     *            Y-Axis, must be perpendicular to point and x
     * @param vertexData
     *            float array to store calculated vertices and normals
     */
    private void createVerticeCircle(Vector3f point, Vector3f x, Vector3f y, float[] vertexData) {
        for (int j = 0; j < vertices; j++) {
            int index = 6 * j;

            float thetasin = sincos[0][j];
            float thetacos = sincos[1][j];

            Vector3f aa = new Vector3f(x);
            Vector3f bb = new Vector3f(y);

            aa.scale(thetasin);
            bb.scale(thetacos);
            aa.add(bb);

            aa.scale(radius);

            Vector3f vertex = new Vector3f(point);
            vertex.add(aa);

            // vertex
            vertexData[index] = vertex.x;
            vertexData[index + 1] = vertex.y;
            vertexData[index + 2] = vertex.z;

            aa.normalize();
            // normal
            vertexData[index + 3] = aa.x;
            vertexData[index + 4] = aa.y;
            vertexData[index + 5] = aa.z;
        }
    }

}
