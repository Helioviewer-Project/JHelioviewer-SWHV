package org.helioviewer.gl3d.model.image;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL2;

import org.helioviewer.base.math.Pair;
import org.helioviewer.gl3d.scenegraph.GL3DShape;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ImageTextureRecapturedReason;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.opengl.GL3DImageTextureView;
import org.helioviewer.viewmodel.view.opengl.GLFilterView;
import org.helioviewer.viewmodel.view.opengl.shader.ShaderFactory;

import com.jogamp.common.nio.Buffers;

/**
 * Maps the solar disc part of an image layer onto an adaptive mesh that either
 * covers the entire solar disc or the just the part that is visible in the view
 * frustum.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DImageSphere extends GL3DShape {

    private final GL3DImageLayer layer;
    private final boolean showSphere;
    private boolean showCorona;
    private final boolean restoreColorMask;
    private final GL3DImageTextureView imageTextureView;
    private int positionBufferID;
    private int indexBufferID;
    private int indexBufferSize;

    private int positionBufferSize;

    public GL3DImageSphere(GL3DImageTextureView imageTextureView, GL3DImageLayer imageLayer, boolean showSphere, boolean showCorona, boolean restoreColorMask) {
        super("Sphere");
        this.imageTextureView = imageTextureView;

        imageTextureView.addViewListener(new ViewListener() {
            @Override
            public void viewChanged(View sender, ChangeEvent aEvent) {
                ImageTextureRecapturedReason reason = aEvent.getLastChangedReasonByType(ImageTextureRecapturedReason.class);
                if (reason != null) {
                    markAsChanged();
                }
            }
        });
        this.markAsChanged();
        this.restoreColorMask = restoreColorMask;
        layer = imageLayer;
        this.showSphere = showSphere;
        this.showCorona = showCorona;
    }

    @Override
    public void shapeDraw(GL3DState state) {
        GL2 gl = state.gl;
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_BLEND);
        GLFilterView glfilter = this.imageTextureView.getAdapter(GLFilterView.class);

        ShaderFactory.bindVertexShader(gl);
        ShaderFactory.bindFragmentShader(gl);
        if (glfilter != null) {
            glfilter.renderGL(gl, true);
        }
        ShaderFactory.filter(gl);

        enablePositionVBO(state);
        enableIndexVBO(state);
        {
            state.gl.glVertexPointer(3, GL2.GL_FLOAT, 3 * Buffers.SIZEOF_FLOAT, 0);
            if (this.showCorona) {
                state.gl.glDepthRange(1.f, 1.f);
                gl.glDrawElements(GL2.GL_TRIANGLES, 6, GL2.GL_UNSIGNED_INT, (this.indexBufferSize - 6) * Buffers.SIZEOF_INT);
                state.gl.glDepthRange(0.f, 1.f);
            }
            if (this.showSphere) {
                gl.glDrawElements(GL2.GL_TRIANGLES, this.indexBufferSize - 6, GL2.GL_UNSIGNED_INT, 0);
            }
        }
        disableIndexVBO(state);
        disablePositionVBO(state);

        if (restoreColorMask) {
            gl.glColorMask(true, true, true, true);
        }
    }

    private int generate(GL3DState state) {
        int[] tmpId = new int[1];
        state.gl.glGenBuffers(1, tmpId, 0);
        return tmpId[0];
    }

    @Override
    public void shapeInit(GL3DState state) {
        Pair<FloatBuffer, IntBuffer> bufferPair = this.makeIcosphere(3);
        FloatBuffer positionBuffer = bufferPair.a;
        IntBuffer indexBuffer = bufferPair.b;

        this.positionBufferSize = positionBuffer.capacity();
        positionBufferID = generate(state);

        state.gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
        state.gl.glBufferData(GL2.GL_ARRAY_BUFFER, this.positionBufferSize * Buffers.SIZEOF_FLOAT, positionBuffer, GL2.GL_STATIC_DRAW);

        indexBufferID = generate(state);
        indexBufferSize = indexBuffer.capacity();
        state.gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferID);
        state.gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * Buffers.SIZEOF_INT, indexBuffer, GL2.GL_STATIC_DRAW);
        this.imageTextureView.forceUpdate();
    }

    private void enableIndexVBO(GL3DState state) {
        state.gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferID);
    }

    private void disableIndexVBO(GL3DState state) {
        state.gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void enablePositionVBO(GL3DState state) {
        state.gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        state.gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
    }

    private void disablePositionVBO(GL3DState state) {
        state.gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        state.gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
    }

    private void deletePositionVBO(GL3DState state) {
        state.gl.glDeleteBuffers(1, new int[] { this.positionBufferID }, 0);
    }

    private void deleteIndexVBO(GL3DState state) {
        state.gl.glDeleteBuffers(1, new int[] { this.indexBufferID }, 0);
    }

    public GL3DImageTextureView getImageTextureView() {
        return imageTextureView;
    }

    @Override
    public void shapeUpdate(GL3DState state) {
    }

    @Override
    public void shapeDelete(GL3DState state) {
        disablePositionVBO(state);
        disableIndexVBO(state);
        deletePositionVBO(state);
        deleteIndexVBO(state);
    }

    private Pair<FloatBuffer, IntBuffer> makeIcosphere(int level) {
        float t = (float) ((Math.sqrt(5) - 1) / 2);
        float[][] icosahedronVertexList = new float[][] { new float[] { -1, -t, 0 }, new float[] { 0, 1, t }, new float[] { 0, 1, -t }, new float[] { 1, t, 0 }, new float[] { 1, -t, 0 }, new float[] { 0, -1, -t }, new float[] { 0, -1, t }, new float[] { t, 0, 1 }, new float[] { -t, 0, 1 }, new float[] { t, 0, -1 }, new float[] { -t, 0, -1 }, new float[] { -1, t, 0 }, };
        for (float[] v : icosahedronVertexList) {
            float length = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            v[0] /= length;
            v[1] /= length;
            v[2] /= length;
        }
        int[][] icosahedronFaceList = new int[][] { { 3, 7, 1 }, { 4, 7, 3 }, { 6, 7, 4 }, { 8, 7, 6 }, { 7, 8, 1 }, { 9, 4, 3 }, { 2, 9, 3 }, { 2, 3, 1 }, { 11, 2, 1 }, { 10, 2, 11 }, { 10, 9, 2 }, { 9, 5, 4 }, { 6, 4, 5 }, { 0, 6, 5 }, { 0, 11, 8 }, { 11, 1, 8 }, { 10, 0, 5 }, { 10, 5, 9 }, { 0, 8, 6 }, { 0, 10, 11 }, };
        ArrayList<Float> vertices = new ArrayList<Float>();
        ArrayList<Integer> faceIndices = new ArrayList<Integer>();
        for (float[] v : icosahedronVertexList) {
            vertices.add(v[0]);
            vertices.add(v[2]);
            vertices.add(v[1]);
        }
        for (int[] f : icosahedronFaceList) {
            subdivide(f[0], f[1], f[2], vertices, faceIndices, level);
        }
        int beginPositionNumberCorona = vertices.size() / 3;
        vertices.add(-40f);
        vertices.add(40f);
        vertices.add(0f);

        vertices.add(40f);
        vertices.add(40f);
        vertices.add(0f);

        vertices.add(40f);
        vertices.add(-40f);
        vertices.add(0f);

        vertices.add(-40f);
        vertices.add(-40f);
        vertices.add(0f);

        faceIndices.add(beginPositionNumberCorona + 0);
        faceIndices.add(beginPositionNumberCorona + 2);
        faceIndices.add(beginPositionNumberCorona + 1);

        faceIndices.add(beginPositionNumberCorona + 2);
        faceIndices.add(beginPositionNumberCorona + 0);
        faceIndices.add(beginPositionNumberCorona + 3);
        FloatBuffer positionBuffer = FloatBuffer.allocate(vertices.size());
        for (Float vert : vertices) {
            positionBuffer.put(vert);
        }
        positionBuffer.flip();
        IntBuffer indexBuffer = IntBuffer.allocate(faceIndices.size());

        for (int i : faceIndices) {
            indexBuffer.put(i);
        }
        indexBuffer.flip();

        return new Pair<FloatBuffer, IntBuffer>(positionBuffer, indexBuffer);
    }

    private void subdivide(int vx, int vy, int vz, ArrayList<Float> vertexList, ArrayList<Integer> faceList, int level) {
        if (level != 0) {
            float x1 = (vertexList.get(3 * vx) + vertexList.get(3 * vy));
            float y1 = (vertexList.get(3 * vx + 1) + vertexList.get(3 * vy + 1));
            float z1 = (vertexList.get(3 * vx + 2) + vertexList.get(3 * vy + 2));
            float length = (float) Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
            x1 /= length;
            y1 /= length;
            z1 /= length;
            int firstIndex = vertexList.size() / 3;
            vertexList.add(x1);
            vertexList.add(y1);
            vertexList.add(z1);

            float x2 = (vertexList.get(3 * vz) + vertexList.get(3 * vy));
            float y2 = (vertexList.get(3 * vz + 1) + vertexList.get(3 * vy + 1));
            float z2 = (vertexList.get(3 * vz + 2) + vertexList.get(3 * vy + 2));
            length = (float) Math.sqrt(x2 * x2 + y2 * y2 + z2 * z2);
            x2 /= length;
            y2 /= length;
            z2 /= length;
            int secondIndex = vertexList.size() / 3;
            vertexList.add(x2);
            vertexList.add(y2);
            vertexList.add(z2);

            float x3 = (vertexList.get(3 * vx) + vertexList.get(3 * vz));
            float y3 = (vertexList.get(3 * vx + 1) + vertexList.get(3 * vz + 1));
            float z3 = (vertexList.get(3 * vx + 2) + vertexList.get(3 * vz + 2));
            length = (float) Math.sqrt(x3 * x3 + y3 * y3 + z3 * z3);
            x3 /= length;
            y3 /= length;
            z3 /= length;
            int thirdIndex = vertexList.size() / 3;
            vertexList.add(x3);
            vertexList.add(y3);
            vertexList.add(z3);

            subdivide(vx, firstIndex, thirdIndex, vertexList, faceList, level - 1);
            subdivide(firstIndex, vy, secondIndex, vertexList, faceList, level - 1);
            subdivide(thirdIndex, secondIndex, vz, vertexList, faceList, level - 1);
            subdivide(firstIndex, secondIndex, thirdIndex, vertexList, faceList, level - 1);
        } else {
            faceList.add(vx);
            faceList.add(vy);
            faceList.add(vz);
        }
    }

    public void setCoronaVisibility(boolean visible) {
        this.showCorona = visible;
    }

}
