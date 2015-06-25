package org.helioviewer.jhv.renderable.components;

import java.awt.Component;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.helioviewer.base.Pair;
import org.helioviewer.base.math.GL3DMat4d;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.filters.FiltersPanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.opengl.GLSLShader;
import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.gui.RenderableType;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;

public class RenderableImageLayer implements Renderable {

    private static boolean showCorona = true;
    private static int nextLayerId = 0;
    private final int layerId;

    public int getLayerId() {
        return layerId;
    }

    private int positionBufferID;
    private int indexBufferID;
    private int indexBufferSize;

    private int positionBufferSize;
    private final View layerView;
    private final RenderableType type;
    private boolean isVisible = true;

    private final GLImage glImage;

    public RenderableImageLayer(View view) {
        type = new RenderableImageType(view.getName());
        layerId = nextLayerId++;
        layerView = view;

        glImage = new GLImage(view.getDefaultLUT());

        ImageViewerGui.getRenderableContainer().addBeforeRenderable(this);

        float opacity = (float) (1. / (1. + Layers.getNumLayers()));
        if (layerView instanceof JHVJP2View) {
            JHVJP2View jp2v = (JHVJP2View) layerView;
            if (jp2v.getName().contains("LASCO") || jp2v.getName().contains("COR")) {
                opacity = 1.f;
            }
        }
        glImage.setOpacity(opacity);
    }

    @Override
    public void init(GL2 gl) {
        glImage.init(gl);
        Pair<FloatBuffer, IntBuffer> bufferPair = makeIcosphere(2);
        FloatBuffer positionBuffer = bufferPair.a;
        IntBuffer indexBuffer = bufferPair.b;

        positionBufferSize = positionBuffer.capacity();
        positionBufferID = generate(gl);

        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, positionBufferSize * Buffers.SIZEOF_FLOAT, positionBuffer, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

        indexBufferID = generate(gl);
        indexBufferSize = indexBuffer.capacity();
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferID);
        gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * Buffers.SIZEOF_INT, indexBuffer, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    @Override
    public void render(GL2 gl) {
        if (!isVisible)
            return;

        GLSLShader.bind(gl);
        {
            gl.glEnable(GL2.GL_CULL_FACE);
            gl.glCullFace(GL2.GL_BACK);

            glImage.applyFilters(gl, imageData, layerView.getPreviousImageData(), layerView.getBaseDifferenceImageData());

            GLSLShader.setViewport(GLInfo.pixelScale[0] * Displayer.getViewportWidth(), GLInfo.pixelScale[1] * Displayer.getViewportHeight());
            if (!RenderableImageLayer.showCorona) {
                GLSLShader.setOuterCutOffRadius(1.);
            }
            GLSLShader.filter(gl);

            GL3DCamera camera = Displayer.getActiveCamera();
            GL3DMat4d vpmi = camera.getOrthoMatrixInverse();
            vpmi.translate(new GL3DVec3d(-camera.getTranslation().x, -camera.getTranslation().y, 0.));
            GLSLShader.bindMatrix(gl, vpmi.getFloatArray());
            GLSLShader.bindCameraDifferenceRotationQuat(gl, camera.getCameraDifferenceRotationQuatd(imageData.getMetaData().getRotationObs()));
            if (glImage.getBaseDifferenceMode()) {
                GLSLShader.bindDiffCameraDifferenceRotationQuat(gl, camera.getCameraDifferenceRotationQuatd(layerView.getBaseDifferenceImageData().getMetaData().getRotationObs()));
            } else if (glImage.getDifferenceMode()) {
                GLSLShader.bindDiffCameraDifferenceRotationQuat(gl, camera.getCameraDifferenceRotationQuatd(layerView.getPreviousImageData().getMetaData().getRotationObs()));
            }

            enablePositionVBO(gl);
            enableIndexVBO(gl);
            {
                gl.glVertexPointer(3, GL2.GL_FLOAT, 3 * Buffers.SIZEOF_FLOAT, 0);
                GLSLShader.bindIsDisc(gl, 0);

                gl.glDepthRange(1.f, 1.f);
                gl.glDrawElements(GL2.GL_TRIANGLES, 6, GL2.GL_UNSIGNED_INT, (indexBufferSize - 6) * Buffers.SIZEOF_INT);
                gl.glDepthRange(0.f, 1.f);

                GLSLShader.bindIsDisc(gl, 1);
                gl.glDrawElements(GL2.GL_TRIANGLES, indexBufferSize - 6, GL2.GL_UNSIGNED_INT, 0);
            }
            disableIndexVBO(gl);
            disablePositionVBO(gl);

            gl.glColorMask(true, true, true, true);
            gl.glDisable(GL2.GL_CULL_FACE);
        }
        GLSLShader.unbind(gl);
    }

    private int generate(GL2 gl) {
        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        return tmpId[0];
    }

    private void enableIndexVBO(GL2 gl) {
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferID);
    }

    private void disableIndexVBO(GL2 gl) {
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void enablePositionVBO(GL2 gl) {
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
    }

    private void disablePositionVBO(GL2 gl) {
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
    }

    private void deletePositionVBO(GL2 gl) {
        gl.glDeleteBuffers(1, new int[] { positionBufferID }, 0);
    }

    private void deleteIndexVBO(GL2 gl) {
        gl.glDeleteBuffers(1, new int[] { indexBufferID }, 0);
    }

    @Override
    public void remove(GL2 gl) {
        Layers.removeLayer(layerView);
        dispose(gl);
    }

    private static Pair<FloatBuffer, IntBuffer> makeIcosphere(int level) {
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
        float r = 40.f;
        vertices.add(-r);
        vertices.add(r);
        vertices.add(0f);

        vertices.add(r);
        vertices.add(r);
        vertices.add(0f);

        vertices.add(r);
        vertices.add(-r);
        vertices.add(0f);

        vertices.add(-r);
        vertices.add(-r);
        vertices.add(0f);

        faceIndices.add(beginPositionNumberCorona + 0);
        faceIndices.add(beginPositionNumberCorona + 2);
        faceIndices.add(beginPositionNumberCorona + 1);

        faceIndices.add(beginPositionNumberCorona + 2);
        faceIndices.add(beginPositionNumberCorona + 0);
        faceIndices.add(beginPositionNumberCorona + 3);
        FloatBuffer positionBuffer = FloatBuffer.allocate(vertices.size());
        for (Float vert : vertices) {
            if (vert == 0f)
                vert = Math.nextAfter(vert, vert + 1.0f);
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

    private static void subdivide(int vx, int vy, int vz, ArrayList<Float> vertexList, ArrayList<Integer> faceList, int level) {
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

    public static void toggleCorona() {
        showCorona = !showCorona;
    }

    @Override
    public RenderableType getType() {
        return type;
    }

    @Override
    public Component getOptionsPanel() {
        FiltersPanel fp = ImageViewerGui.getFiltersPanel();
        fp.setActiveImage(glImage);
        return fp;
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisible(boolean _isVisible) {
        isVisible = _isVisible;
    }

    @Override
    public String getName() {
        return layerView.getName();
    }

    @Override
    public String getTimeString() {
        return imageData.getMetaData().getDateObs().getCachedDate();
    }

    public View getMainLayerView() {
        return layerView;
    }

    @Override
    public boolean isDeletable() {
        return true;
    }

    @Override
    public boolean isActiveImageLayer() {
        return Layers.getActiveView() == layerView;
    }

    @Override
    public void dispose(GL2 gl) {
        disablePositionVBO(gl);
        disableIndexVBO(gl);
        deletePositionVBO(gl);
        deleteIndexVBO(gl);
        glImage.dispose(gl);
    }

    private ImageData imageData;

    public void setImageData(ImageData _imageData) {
        imageData = _imageData;
    }

    public ImageData getImageData() {
        return imageData;
    }

}
