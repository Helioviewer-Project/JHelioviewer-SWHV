package org.helioviewer.jhv.renderable.components;

import java.awt.Component;
import java.awt.geom.Rectangle2D;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.Viewport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.filters.FiltersPanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.opengl.GLSLShader;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ImageDataHandler;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.threads.JHVWorker;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableImageLayer extends AbstractRenderable implements ImageDataHandler {

    private static boolean showCorona = true;

    private int positionBufferID;
    private int indexBufferID;
    private int indexBufferSize;
    private final GLImage glImage = new GLImage();

    private JHVWorker<?, ?> worker;
    private View view;

    private static final double vpScale = 0.035;
    private static final String loading = "Loading...";

    public RenderableImageLayer(JHVWorker<?, ?> _worker) {
        worker = _worker;
        setVisible(true);
    }

    public void setView(View _view) {
        if (view != null)
            return;
        view = _view;
        worker = null; // drop reference

        float opacity = 1;
        if (!view.getName().contains("LASCO") && !view.getName().contains("COR")) {
            int count = 0;
            for (int i = 0; i < Layers.getNumLayers(); i++) {
                String name = Layers.getLayer(i).getName();
                if (!name.contains("LASCO") && !name.contains("COR"))
                    count++;
            }
            opacity = (float) (1. / (1 + count));
        }
        glImage.setOpacity(opacity);
        glImage.setLUT(view.getDefaultLUT(), false);

        view.setImageLayer(this);
        view.setDataHandler(this);
        Layers.addLayer(view);
        ImageViewerGui.getRenderableContainer().fireListeners();
    }

    @Override
    public void setVisible(boolean isVisible) {
        super.setVisible(isVisible);
        if (Displayer.multiview) {
            ImageViewerGui.getRenderableContainer().arrangeMultiView(true);
        }
    }

    @Override
    public void init(GL2 gl) {
        glImage.init(gl);
        Pair<FloatBuffer, IntBuffer> bufferPair = makeIcosphere(2);
        FloatBuffer positionBuffer = bufferPair.a;
        IntBuffer indexBuffer = bufferPair.b;

        int positionBufferSize = positionBuffer.capacity();
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
    public void remove(GL2 gl) {
        if (view != null) {
            view.setImageLayer(null);
            Layers.removeLayer(view);
            view = null;
        }
        if (worker != null)
            worker.cancel(true);

        imageData = prevImageData = baseImageData = null;
        dispose(gl);
    }

    @Override
    public void prerender(GL2 gl) {
        if (imageData == null) {
            return;
        }
        glImage.streamImage(gl, imageData, prevImageData, baseImageData);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        _render(camera, vp, gl, new double[] { 1., 1., 0., 1. });
    }

    @Override
    public void renderMiniview(Camera camera, Viewport vp, GL2 gl) {
        _render(camera, vp, gl, new double[] { 0., 0., 0., 0. });
    }

    private void _render(Camera camera, Viewport vp, GL2 gl, double[] depthrange) {
        if (imageData == null) {
            return;
        }
        if (!isVisible[vp.index])
            return;

        GLSLShader.bind(gl);
        {
            glImage.applyFilters(gl, imageData, prevImageData, baseImageData);

            GLSLShader.setViewport(vp.x, vp.y, vp.width, vp.height);
            if (!RenderableImageLayer.showCorona) {
                GLSLShader.setOuterCutOffRadius(1.);
            }
            GLSLShader.filter(gl);

            camera.push(imageData.getMasterTime());

            Mat4 vpmi = CameraHelper.getOrthoMatrixInverse(camera, vp);
            vpmi.translate(new Vec3(-camera.getCurrentTranslation().x, -camera.getCurrentTranslation().y, 0.));

            GLSLShader.bindMatrix(gl, vpmi.getFloatArray());
            GLSLShader.bindCameraDifferenceRotationQuat(gl, CameraHelper.getCameraDifferenceRotation(camera, imageData.getMetaData().getRotationObs()));
            if (glImage.getBaseDifferenceMode()) {
                GLSLShader.bindDiffCameraDifferenceRotationQuat(gl, CameraHelper.getCameraDifferenceRotation(camera, baseImageData.getMetaData().getRotationObs()));
            } else if (glImage.getDifferenceMode()) {
                GLSLShader.bindDiffCameraDifferenceRotationQuat(gl, CameraHelper.getCameraDifferenceRotation(camera, prevImageData.getMetaData().getRotationObs()));
            }

            camera.pop();

            enablePositionVBO(gl);
            enableIndexVBO(gl);
            {
                gl.glVertexPointer(3, GL2.GL_FLOAT, 3 * Buffers.SIZEOF_FLOAT, 0);

                GLSLShader.bindIsDisc(gl, 1);
                gl.glDepthRange(depthrange[2], depthrange[3]);
                gl.glDrawElements(GL2.GL_TRIANGLES, indexBufferSize - 6, GL2.GL_UNSIGNED_INT, 0);

                GLSLShader.bindIsDisc(gl, 0);
                gl.glDepthRange(depthrange[0], depthrange[1]);
                gl.glDrawElements(GL2.GL_TRIANGLES, 6, GL2.GL_UNSIGNED_INT, (indexBufferSize - 6) * Buffers.SIZEOF_INT);

                gl.glDepthRange(0, 1);
            }
            disableIndexVBO(gl);
            disablePositionVBO(gl);

            gl.glColorMask(true, true, true, true);
        }
        GLSLShader.unbind(gl);
    }

    @Override
    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
        if (imageData == null) {
            int delta = (int) (vp.height * 0.01);
            TextRenderer renderer = GLText.getRenderer(Math.min(36, (int) (vp.height * vpScale)));
            Rectangle2D rect = renderer.getBounds(loading);

            renderer.beginRendering(vp.width, vp.height, true);
            renderer.draw(loading, (int) (vp.width - rect.getWidth() - delta), (int) (vp.height - rect.getHeight() - delta));
            renderer.endRendering();
        }
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
    public Component getOptionsPanel() {
        if (view == null)
            return null;

        FiltersPanel fp = ImageViewerGui.getFiltersPanel();
        fp.setActiveImage(glImage);
        fp.setView(view);
        return fp;
    }

    @Override
    public String getName() {
        return view == null ? loading : view.getName();
    }

    @Override
    public String getTimeString() {
        if (imageData == null) {
            return "N/A";
        }
        return imageData.getMetaData().getDateObs().toString();
    }

    @Override
    public boolean isDeletable() {
        return true;
    }

    @Override
    public void dispose(GL2 gl) {
        disablePositionVBO(gl);
        disableIndexVBO(gl);
        deletePositionVBO(gl);
        deleteIndexVBO(gl);
        glImage.dispose(gl);
    }

    public boolean isActiveImageLayer() {
        return Layers.getActiveView() == view;
    }

    public void setActiveImageLayer() {
        if (view != null)
            Layers.setActiveView(view);
    }

    private ImageData imageData;
    private ImageData prevImageData;
    private ImageData baseImageData;

    private void setImageData(ImageData newImageData) {
        int frame = newImageData.getFrameNumber();
        if (frame == 0) {
            baseImageData = newImageData;
        }

        if (imageData == null || (prevImageData != null && prevImageData.getFrameNumber() - frame > 2)) {
            prevImageData = newImageData;
        } else if (frame != imageData.getFrameNumber()) {
            prevImageData = imageData;
        }

        imageData = newImageData;
    }

    public ImageData getImageData() {
        return imageData;
    }

    public GLImage getGLImage() {
        return glImage;
    }

    @Override
    public void handleData(ImageData imageData) {
        setImageData(imageData);
        ImageViewerGui.getRenderableContainer().fireTimeUpdated(this);
        Displayer.display();
    }

}
