package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.helioviewer.jhv.base.logging.Log;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;

public class GLLine {

    private float[][] points;
    private float[][] colors;

    private int[] vboAttribRefs;
    private int[] vboAttribLens = { 3, 3, 3, 1, 4 };

    private VBO[] vbos = new VBO[5];
    private VBO ivbo;

    public void setData(GL2 gl, FloatBuffer vertices, FloatBuffer _colors) {
        if (vertices.limit() / 3 != _colors.limit() / 4 || vertices.limit() / 3 < 2) {
            Log.error("Something is wrong with the vertices or colors from this line.");
            return;
        }
        points = monoToBidi(vertices, vertices.limit() / 3, 3);
        colors = monoToBidi(_colors, _colors.limit() / 4, 4);
        setBufferData(gl);
    }

    private float[][] monoToBidi(final FloatBuffer array, final int rows, final int cols) {
        if (array.limit() != rows * cols)
            throw new IllegalArgumentException("Invalid array length");

        float[][] bidi = new float[rows][cols];
        int c = 0;
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                bidi[i][j] = array.get(c++);
        return bidi;
    }

    public void render(GL2 gl, double aspect, double thickness) {
        if (points == null)
            return;

        GLSLLineShader.line.bind(gl);
        GLSLLineShader.line.setAspect(aspect);
        GLSLLineShader.line.setThickness(thickness);
        GLSLLineShader.line.bindParams(gl);

        bindVBOs(gl);
        gl.glDrawElements(GL2.GL_TRIANGLES, vbos[4].bufferSize, GL2.GL_UNSIGNED_INT, 0);
        unbindVBOs(gl);

        GLSLShader.unbind(gl);
    }

    private void bindVBOs(GL2 gl) {
        for (VBO vbo : vbos) {
            vbo.bindArray(gl);
        }
        ivbo.bindArray(gl);
    }

    private void unbindVBOs(GL2 gl) {
        for (int i = vbos.length - 1; i >= 0; i--) {
            vbos[i].unbindArray(gl);
        }
        ivbo.unbindArray(gl);
    }

    private void initVBOs(GL2 gl) {
        for (int i = 0; i < vboAttribRefs.length; i++) {
            vbos[i] = VBO.gen_float_VBO(vboAttribRefs[i], vboAttribLens[i]);
            vbos[i].init(gl);
        }
        ivbo = VBO.gen_index_VBO();
        ivbo.init(gl);
    }

    private void disposeVBOs(GL2 gl) {
        for (int i = 0; i < vbos.length; i++) {
            vbos[i].dispose(gl);
            vbos[i] = null;
        }
        ivbo.dispose(gl);
        ivbo = null;
    }

    private IntBuffer gen_indices(int length) {
        IntBuffer indicesBuffer = IntBuffer.allocate(6 * points.length);
        for (int j = 0; j < 2 * length - 3; j = j + 2) {
            indicesBuffer.put(j + 0);
            indicesBuffer.put(j + 1);
            indicesBuffer.put(j + 2);
            indicesBuffer.put(j + 2);
            indicesBuffer.put(j + 1);
            indicesBuffer.put(j + 3);
        }
        indicesBuffer.flip();
        return indicesBuffer;
    }

    private void addPoint(FloatBuffer buffer, float[] point) {
        buffer.put(point);
        buffer.put(point);
    }

    public void init(GL2 gl) {
        vboAttribRefs = new int[] { GLSLLineShader.previousLineRef, GLSLLineShader.lineRef, GLSLLineShader.nextLineRef,
                GLSLLineShader.directionRef, GLSLLineShader.linecolorRef };
        initVBOs(gl);
    }

    private void setBufferData(GL2 gl) {
        FloatBuffer previousLineBuffer = FloatBuffer.allocate(3 * 2 * points.length);
        FloatBuffer lineBuffer = FloatBuffer.allocate(3 * 2 * points.length);
        FloatBuffer nextLineBuffer = FloatBuffer.allocate(3 * 2 * points.length);
        FloatBuffer directionBuffer = FloatBuffer.allocate(2 * 2 * points.length);
        FloatBuffer colorBuffer = FloatBuffer.allocate(2 * 4 * points.length);
        int dir = -1;
        for (int i = 0; i < 2 * points.length; i++) {
            directionBuffer.put(dir);
            directionBuffer.put(-dir);
        }

        addPoint(previousLineBuffer, points[0]);
        addPoint(lineBuffer, points[0]);
        addPoint(nextLineBuffer, points[1]);
        addPoint(colorBuffer, colors[0]);
        for (int i = 1; i < points.length - 1; i++) {
            addPoint(previousLineBuffer, points[i - 1]);
            addPoint(lineBuffer, points[i]);
            addPoint(nextLineBuffer, points[i + 1]);
            addPoint(colorBuffer, colors[i]);
        }
        addPoint(previousLineBuffer, points[points.length - 2]);
        addPoint(lineBuffer, points[points.length - 1]);
        addPoint(nextLineBuffer, points[points.length - 1]);
        addPoint(colorBuffer, colors[points.length - 1]);

        previousLineBuffer.flip();
        lineBuffer.flip();
        nextLineBuffer.flip();
        directionBuffer.flip();
        colorBuffer.flip();

        vbos[0].bindBufferData(gl, previousLineBuffer, Buffers.SIZEOF_FLOAT);
        vbos[1].bindBufferData(gl, lineBuffer, Buffers.SIZEOF_FLOAT);
        vbos[2].bindBufferData(gl, nextLineBuffer, Buffers.SIZEOF_FLOAT);
        vbos[3].bindBufferData(gl, directionBuffer, Buffers.SIZEOF_FLOAT);
        vbos[4].bindBufferData(gl, colorBuffer, Buffers.SIZEOF_FLOAT);

        IntBuffer indexBuffer = gen_indices(points.length);
        ivbo.bindBufferData(gl, indexBuffer, Buffers.SIZEOF_INT);
    }

    public void dispose(GL2 gl) {
        disposeVBOs(gl);
    }

}
