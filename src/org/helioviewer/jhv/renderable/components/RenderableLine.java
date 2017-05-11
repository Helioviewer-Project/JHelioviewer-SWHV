package org.helioviewer.jhv.renderable.components;

import java.awt.Component;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.GLSLLineShader;
import org.helioviewer.jhv.opengl.GLSLShader;
import org.helioviewer.jhv.opengl.VBO;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;

public class RenderableLine extends AbstractRenderable {
    private float[][] points;
    private float[][] colors;

    private int[] vboAttribRefs = { GLSLLineShader.previousLineRef, GLSLLineShader.lineRef, GLSLLineShader.nextLineRef,
            GLSLLineShader.directionRef, GLSLLineShader.linecolorRef };
    private int[] vboAttribLens = { 3, 3, 3, 1, 4 };

    private VBO[] vbos = new VBO[5];
    private VBO ivbo;

    private boolean inited = false;

    public RenderableLine(float[][] _points) {
        points = _points;
    }

    public RenderableLine(FloatBuffer vertices, FloatBuffer _colors) {
        points = monoToBidi(vertices, vertices.limit() / 3, 3);
        colors = monoToBidi(_colors, _colors.limit() / 4, 4);
    }

    public float[][] monoToBidi(final FloatBuffer array, final int rows, final int cols) {
        if (array.limit() != (rows * cols))
            throw new IllegalArgumentException("Invalid array length");

        float[][] bidi = new float[rows][cols];
        int c = 0;
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                bidi[i][j] = array.get(c++);
        return bidi;
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!inited)
            return;

        GLSLLineShader.line.bind(gl);
        GLSLLineShader.line.setAspect((float) vp.aspect * 2);
        GLSLLineShader.line.bindParams(gl);

        bindVBOs(gl);

        gl.glDrawElements(GL2.GL_TRIANGLES, 6, GL2.GL_UNSIGNED_INT, 0);
        gl.glDrawElements(GL2.GL_TRIANGLES, vbos[4].bufferSize, GL2.GL_UNSIGNED_INT, 3);

        unbindVBOs(gl);

        GLSLShader.unbind(gl);
    }

    @Override
    public void remove(GL2 gl) {
        GLSLLineShader.dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return null;
    }

    @Override
    public String getName() {
        return "line";
    }

    @Override
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    private void bindVBOs(GL2 gl) {
        for (int i = 0; i < vbos.length; i++) {
            vbos[i].bindArray(gl);
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
            vbos[i] = new VBO(GL2.GL_ARRAY_BUFFER, vboAttribRefs[i], vboAttribLens[i]);
            vbos[i].init(gl);
        }
        ivbo = new VBO(GL2.GL_ELEMENT_ARRAY_BUFFER, -1, -1);
    }

    private void disposeVBOs(GL2 gl) {
        for (int i = 0; i < vbos.length; i++) {
            vbos[i].dispose(gl);
            vbos[i] = null;
        }
        ivbo.dispose(gl);
        ivbo = null;
    }

    public IntBuffer gen_indices(int length) {
        IntBuffer indicesBuffer = IntBuffer.allocate(6 * points.length);
        int i = 0;
        for (int j = 0; j < length; j++) {
            indicesBuffer.put(i + 0);
            indicesBuffer.put(i + 1);
            indicesBuffer.put(i + 2);
            indicesBuffer.put(i + 2);
            indicesBuffer.put(i + 1);
            indicesBuffer.put(i + 3);
            i = i + 2;
        }
        indicesBuffer.flip();
        return indicesBuffer;
    }

    @Override
    public void init(GL2 gl) {
        if (points.length < 2)
            return;
        GLSLLineShader.init(gl);

        FloatBuffer previousLineBuffer = FloatBuffer.allocate(6 * points.length);
        FloatBuffer lineBuffer = FloatBuffer.allocate(6 * points.length);
        FloatBuffer nextLineBuffer = FloatBuffer.allocate(6 * points.length);
        FloatBuffer directionBuffer = FloatBuffer.allocate(4 * points.length);
        FloatBuffer colorBuffer = FloatBuffer.allocate(2 * 4 * points.length);

        int dir = -1;
        for (int i = 0; i < 2 * points.length; i++) {
            directionBuffer.put(dir);
            directionBuffer.put(-dir);
        }

        previousLineBuffer.put(points[0]);
        previousLineBuffer.put(points[0]);
        lineBuffer.put(points[0]);
        lineBuffer.put(points[0]);
        colorBuffer.put(colors[0]);
        colorBuffer.put(colors[0]);
        nextLineBuffer.put(points[1]);
        nextLineBuffer.put(points[1]);
        for (int i = 1; i < points.length - 1; i++) {
            previousLineBuffer.put(points[i - 1]);
            previousLineBuffer.put(points[i - 1]);
            lineBuffer.put(points[i]);
            lineBuffer.put(points[i]);
            colorBuffer.put(colors[i]);
            colorBuffer.put(colors[i]);
            nextLineBuffer.put(points[i + 1]);
            nextLineBuffer.put(points[i + 1]);
        }
        previousLineBuffer.put(points[points.length - 2]);
        previousLineBuffer.put(points[points.length - 2]);
        lineBuffer.put(points[points.length - 1]);
        lineBuffer.put(points[points.length - 1]);
        colorBuffer.put(colors[points.length - 1]);
        colorBuffer.put(colors[points.length - 1]);
        nextLineBuffer.put(points[points.length - 1]);
        nextLineBuffer.put(points[points.length - 1]);

        previousLineBuffer.flip();
        lineBuffer.flip();
        nextLineBuffer.flip();
        directionBuffer.flip();
        colorBuffer.flip();

        initVBOs(gl);
        vbos[0].bindBufferData(gl, previousLineBuffer, Buffers.SIZEOF_FLOAT);
        vbos[1].bindBufferData(gl, lineBuffer, Buffers.SIZEOF_FLOAT);
        vbos[2].bindBufferData(gl, nextLineBuffer, Buffers.SIZEOF_FLOAT);
        vbos[3].bindBufferData(gl, directionBuffer, Buffers.SIZEOF_FLOAT);
        vbos[4].bindBufferData(gl, colorBuffer, Buffers.SIZEOF_FLOAT);

        IntBuffer indexBuffer = this.gen_indices(points.length);
        ivbo.bindBufferData(gl, indexBuffer, Buffers.SIZEOF_INT);
        inited = true;

    }

    @Override
    public void dispose(GL2 gl) {
        this.disposeVBOs(gl);
        inited = false;
    }

}
