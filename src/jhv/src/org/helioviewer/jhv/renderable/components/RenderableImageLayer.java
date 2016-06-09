package org.helioviewer.jhv.renderable.components;

import java.awt.Component;
import java.awt.geom.Rectangle2D;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.opengl.GLSLSolarShader;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.renderable.components.ImageLayer.FiltersPanel;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ImageDataHandler;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableImageLayer extends AbstractRenderable implements ImageDataHandler {

    private static boolean showCorona = true;

    private int positionBufferID;
    private int indexBufferID;
    private int indexBufferSize;
    private final GLImage glImage = new GLImage();
    private final FiltersPanel filtersPanel = new FiltersPanel(this);

    private JHVWorker<?, ?> worker;
    private View view;

    private static final double vpScale = 0.035;
    private static final String loading = "Loading...";

    public RenderableImageLayer(JHVWorker<?, ?> _worker) {
        worker = _worker;
        setVisible(true);
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
    public void setVisible(boolean isVisible) {
        super.setVisible(isVisible);
        if (Displayer.multiview) {
            ImageViewerGui.getRenderableContainer().arrangeMultiView(true);
        }
    }

    public void setView(View _view) {
        if (view != null)
            return;

        view = _view;
        worker = null; // drop reference

        float opacity = 1;

        if (!Displayer.multiview) {
            if (!view.getName().contains("LASCO") && !view.getName().contains("COR")) {
                int count = 0;
                for (int i = 0; i < Layers.getNumLayers(); i++) {
                    String name = Layers.getLayer(i).getName();
                    if (!name.contains("LASCO") && !name.contains("COR"))
                        count++;
                }
                opacity = (float) (1. / (1 + count));
            }
        }

        glImage.setOpacity(opacity);
        glImage.setLUT(view.getDefaultLUT(), false);

        view.setImageLayer(this);
        view.setDataHandler(this);
        Layers.addLayer(view);
        ImageViewerGui.getRenderableContainer().fireListeners();

        filtersPanel.setActiveImage(glImage);
        filtersPanel.setView(view);

        if (Displayer.multiview) {
            ImageViewerGui.getRenderableContainer().arrangeMultiView(true);
        }
    }

    @Override
    public void remove(GL2 gl) {
        filtersPanel.setActiveImage(null);
        filtersPanel.setView(null);

        if (view != null) {
            Layers.removeLayer(view);
            view.setDataHandler(null);
            view.setImageLayer(null);
            view = null;

            if (Displayer.multiview) {
                ImageViewerGui.getRenderableContainer().arrangeMultiView(true);
            }
        }
        if (worker != null)
            worker.cancel(true);

        imageData = prevImageData = baseImageData = null;
        dispose(gl);
    }

    public void setRender(Camera camera, Viewport vp, double factor) {
        if (view != null)
            view.render(camera, vp, factor);
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
        _render(camera, vp, gl, new double[] { 1., 1., 0., 1. }, GLSLSolarShader.ortho, null);
    }

    @Override
    public void renderMiniview(Camera camera, Viewport vp, GL2 gl) {
        _render(camera, vp, gl, new double[] { 0., 0., 0., 0. }, GLSLSolarShader.ortho, null);
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl, GLSLSolarShader shader, GridScale scale) {
        _render(camera, vp, gl, new double[] { 1., 1., 1., 1. }, shader, scale);
    }

    private void _render(Camera camera, Viewport vp, GL2 gl, double[] depthrange, GLSLSolarShader shader, GridScale scale) {
        if (imageData == null) {
            return;
        }
        if (!isVisible[vp.idx])
            return;

        shader.bind(gl);
        {
            glImage.applyFilters(gl, imageData, prevImageData, baseImageData, shader);

            shader.setViewport(vp.x, vp.yGL, vp.width, vp.height);
            if (!RenderableImageLayer.showCorona) {
                shader.setOuterCutOffRadius(1.);
            }
            shader.filter(gl);

            camera.push(imageData.getViewpoint());

            Mat4 vpmi = CameraHelper.getOrthoMatrixInverse(camera, vp);
            if (Displayer.mode != Displayer.DisplayMode.ORTHO)
                vpmi.translate(new Vec3(-camera.getCurrentTranslation().x / vp.aspect, -camera.getCurrentTranslation().y, 0.));
            else
                vpmi.translate(new Vec3(-camera.getCurrentTranslation().x, -camera.getCurrentTranslation().y, 0.));

            Quat q = camera.getRotation();
            shader.bindMatrix(gl, vpmi.getFloatArray());
            shader.bindCameraDifferenceRotationQuat(gl, Quat.rotateWithConjugate(q, imageData.getMetaData().getViewpoint().orientation));
            if (glImage.getBaseDifferenceMode()) {
                shader.bindDiffCameraDifferenceRotationQuat(gl, Quat.rotateWithConjugate(q, baseImageData.getMetaData().getViewpoint().orientation));
            } else if (glImage.getDifferenceMode()) {
                shader.bindDiffCameraDifferenceRotationQuat(gl, Quat.rotateWithConjugate(q, prevImageData.getMetaData().getViewpoint().orientation));
            }
            shader.bindAngles(gl, imageData.getMetaData().getViewpointL());
            if (scale != null)
                shader.setPolarRadii(gl, scale.getYstart(), scale.getYstop());
            camera.pop();

            enablePositionVBO(gl);
            enableIndexVBO(gl);
            {
                gl.glVertexPointer(3, GL2.GL_FLOAT, 3 * Buffers.SIZEOF_FLOAT, 0);
                if (shader == GLSLSolarShader.ortho) {
                    shader.bindIsDisc(gl, 1);
                    gl.glDepthRange(depthrange[2], depthrange[3]);
                    gl.glDrawElements(GL2.GL_TRIANGLES, indexBufferSize - 6, GL2.GL_UNSIGNED_INT, 0);
                    shader.bindIsDisc(gl, 0);
                }
                gl.glDepthRange(depthrange[0], depthrange[1]);
                gl.glDrawElements(GL2.GL_TRIANGLES, 6, GL2.GL_UNSIGNED_INT, (indexBufferSize - 6) * Buffers.SIZEOF_INT);

                gl.glDepthRange(0, 1);
            }
            disableIndexVBO(gl);
            disablePositionVBO(gl);

            gl.glColorMask(true, true, true, true);
        }
        shader.unbind(gl);
    }

    @Override
    public void renderFullFloat(Camera camera, Viewport vp, GL2 gl) {
        if (imageData == null) {
            int delta = (int) (vp.height * 0.01);
            TextRenderer renderer = GLText.getRenderer(GLText.TEXT_SIZE_LARGE);
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

        faceIndices.add(beginPositionNumberCorona);
        faceIndices.add(beginPositionNumberCorona + 2);
        faceIndices.add(beginPositionNumberCorona + 1);

        faceIndices.add(beginPositionNumberCorona + 2);
        faceIndices.add(beginPositionNumberCorona);
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
            float x1 = vertexList.get(3 * vx) + vertexList.get(3 * vy);
            float y1 = vertexList.get(3 * vx + 1) + vertexList.get(3 * vy + 1);
            float z1 = vertexList.get(3 * vx + 2) + vertexList.get(3 * vy + 2);
            float length = (float) Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
            x1 /= length;
            y1 /= length;
            z1 /= length;
            int firstIndex = vertexList.size() / 3;
            vertexList.add(x1);
            vertexList.add(y1);
            vertexList.add(z1);

            float x2 = vertexList.get(3 * vz) + vertexList.get(3 * vy);
            float y2 = vertexList.get(3 * vz + 1) + vertexList.get(3 * vy + 1);
            float z2 = vertexList.get(3 * vz + 2) + vertexList.get(3 * vy + 2);
            length = (float) Math.sqrt(x2 * x2 + y2 * y2 + z2 * z2);
            x2 /= length;
            y2 /= length;
            z2 /= length;
            int secondIndex = vertexList.size() / 3;
            vertexList.add(x2);
            vertexList.add(y2);
            vertexList.add(z2);

            float x3 = vertexList.get(3 * vx) + vertexList.get(3 * vz);
            float y3 = vertexList.get(3 * vx + 1) + vertexList.get(3 * vz + 1);
            float z3 = vertexList.get(3 * vx + 2) + vertexList.get(3 * vz + 2);
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

    public void setOpacity(float opacity) {
        glImage.setOpacity(opacity);
        filtersPanel.refresh();
    }

    @Override
    public Component getOptionsPanel() {
        if (view == null)
            return null;
        return filtersPanel;
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
        return imageData.getMetaData().getViewpoint().time.toString();
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
        int frame = newImageData.getMetaData().getFrameNumber();
        if (frame == 0) {
            baseImageData = newImageData;
        }

        if (imageData == null || (prevImageData != null && prevImageData.getMetaData().getFrameNumber() - frame > 2)) {
            prevImageData = newImageData;
        } else if (frame != imageData.getMetaData().getFrameNumber()) {
            prevImageData = imageData;
        }

        imageData = newImageData;
    }

    public ImageData getImageData() {
        return imageData;
    }

    public MetaData getMetaData() {
        if (imageData == null) // not yet decoded
            return view.getMetaData(new JHVDate(0));
        else
            return imageData.getMetaData();
    }

    @Override
    public void handleData(ImageData imageData) {
        setImageData(imageData);
        ImageViewerGui.getRenderableContainer().fireTimeUpdated(this);
        Displayer.display();
    }

    public GLImage getGLImage() {
        return glImage;
    }

}
