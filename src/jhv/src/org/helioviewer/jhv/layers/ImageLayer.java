package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.math.IcoSphere;
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
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.LoadRemoteTask;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.opengl.GLSLShader;
import org.helioviewer.jhv.opengl.GLSLSolarShader;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ImageDataHandler;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class ImageLayer extends AbstractRenderable implements ImageDataHandler {

    private int positionBufferID;
    private int indexBufferID;
    private int indexBufferSize;
    private final GLImage glImage = new GLImage();
    private final ImageLayerOptions optionsPanel;

    private LoadRemoteTask worker;
    private View view;

    private static final String loading = "Loading...";

    public static ImageLayer createImageLayer() {
        ImageLayer imageLayer = new ImageLayer();
        ImageViewerGui.getRenderableContainer().addBeforeRenderable(imageLayer);
        return imageLayer;
    }

    public void load(APIRequest req) {
        if (!req.equals(getAPIRequest())) {
            if (worker != null)
                worker.cancel(true);
            worker = new LoadRemoteTask(this, req, 0);
            JHVGlobals.getExecutorService().execute(worker);
        }
    }

    public void unload() {
        if (view == null) // not changing view
            ImageViewerGui.getRenderableContainer().removeRenderable(this);
        else {
            worker = null;
            Displayer.display();
        }
    }

    private ImageLayer() {
        optionsPanel = new ImageLayerOptions(this);
    }

    @Override
    public void init(GL2 gl) {
        glImage.init(gl);

        FloatBuffer positionBuffer = IcoSphere.IcoSphere.a;
        IntBuffer indexBuffer = IcoSphere.IcoSphere.b;

        positionBufferID = generate(gl);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, positionBuffer.capacity() * Buffers.SIZEOF_FLOAT, positionBuffer, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

        indexBufferID = generate(gl);
        indexBufferSize = indexBuffer.capacity();
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferID);
        gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferSize * Buffers.SIZEOF_INT, indexBuffer, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    @Override
    public void setVisible(boolean isVisible) {
        super.setVisible(isVisible);
        if (Displayer.multiview) {
            Layers.arrangeMultiView(true);
        }
    }

    private float opacity = -1;

    public void setView(View _view) {
        if (view != null)
            unsetView();

        view = _view;
        worker = null; // drop reference

        setVisible(true); // enable optionsPanel
        ImageViewerGui.getRenderableContainerPanel().revalidate();
        ImageViewerGui.getRenderableContainerPanel().repaint();

        view.setImageLayer(this);
        view.setDataHandler(this);
        Layers.addLayer(view);
        ImageViewerGui.getRenderableContainer().fireListeners();

        if (Displayer.multiview) {
            Layers.arrangeMultiView(true);
        } else if (opacity == -1) { // first time
            if (Layers.isCor(view.getName()))
                opacity = 1;
            else {
                int count = 0;
                for (int i = 0; i < Layers.getNumLayers(); i++) {
                    if (!Layers.isCor(Layers.getLayer(i).getName()))
                        count++;
                }
                opacity = (float) (1. / (count == 0 ? 1 : count /* satisfy coverity */));
            }
            optionsPanel.setOpacity(opacity);
        }
        optionsPanel.setLUT(view.getDefaultLUT());
    }

    private void unsetView() {
        if (view != null) {
            Layers.removeLayer(view);
            view.setDataHandler(null);
            view.setImageLayer(null);
            view.abolish();
            view = null;
            ImageViewerGui.getRenderableContainer().fireListeners();
        }
        imageData = prevImageData = baseImageData = null;
    }

    @Override
    public void remove(GL2 gl) {
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        unsetView();
        if (Displayer.multiview) {
            Layers.arrangeMultiView(true);
        }
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
            shader.filter(gl);

            camera.push(imageData.getViewpoint());

            Mat4 vpmi = CameraHelper.getOrthoMatrixInverse(camera, vp);
            if (Displayer.mode == Displayer.DisplayMode.ORTHO)
                vpmi.translate(new Vec3(-camera.getCurrentTranslation().x, -camera.getCurrentTranslation().y, 0.));
            else
                vpmi.translate(new Vec3(-camera.getCurrentTranslation().x / vp.aspect, -camera.getCurrentTranslation().y, 0.));

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
        GLSLShader.unbind(gl);
    }

    @Override
    public void renderFullFloat(Camera camera, Viewport vp, GL2 gl) {
        if (imageData == null || worker != null) { // loading something
            int delta = (int) (vp.height * 0.01);
            TextRenderer renderer = GLText.getRenderer(GLText.TEXT_SIZE_LARGE);
            Rectangle2D rect = renderer.getBounds(loading);

            renderer.beginRendering(vp.width, vp.height, true);
            renderer.draw(loading, (int) (vp.width - rect.getWidth() - delta), (int) (vp.height - rect.getHeight() - delta));
            renderer.endRendering();
        }
    }

    private static int generate(GL2 gl) {
        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        return tmpId[0];
    }

    private void enableIndexVBO(GL2 gl) {
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferID);
    }

    private static void disableIndexVBO(GL2 gl) {
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void enablePositionVBO(GL2 gl) {
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
    }

    private static void disablePositionVBO(GL2 gl) {
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
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return view == null || worker != null ? loading : view.getName();
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

    private boolean autoContrast = false;
    private static final double CONTRAST_F1 = 0.001;
    private static final double CONTRAST_F2 = 128 + 64 + 32;

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

        if (autoContrast && imageData.getBuffer() instanceof ByteBuffer) {
            autoContrast(((ByteBuffer) imageData.getBuffer()).array());
        }
    }

    private static void autoContrast(byte[] ba) {
        int len = ba.length;
        int[] histogram = new int[256];
        for (int i = 0; i < len; i++) {
            histogram[getUnsigned(ba[i])]++;
        }

        long ct = 0;
        int j;
        for (j = 255; j >= 0; j--) {
            ct += histogram[j];
            if (ct > CONTRAST_F1 * len) {
                break;
            }
        }

        double factor = CONTRAST_F2 / j;
        // System.out.println(">> " + factor + " " + j);
        if (j != 0 && factor > 1) {
            if (factor > 2)
                factor = 2;
            for (int i = 0; i < len; i++) {
                ba[i] = clipByte((int) (getUnsigned(ba[i]) * factor + 0.5));
            }
        }
    }

    private static int getUnsigned(byte b) {
        return (b + 256) & 0xFF;
    }

    private static byte clipByte(int val) {
        return (byte) (val > 255 ? 255 : val & 0xFF);
    }

    public ImageData getImageData() {
        return imageData;
    }

    public MetaData getMetaData() {
        return imageData == null ? view.getMetaData(new JHVDate(0)) : imageData.getMetaData();
    }

    @Override
    public void handleData(ImageData newImageData) {
        setImageData(newImageData);
        ImageViewerGui.getRenderableContainer().fireTimeUpdated(this);
        Displayer.display();
    }

    @Override
    public boolean isDownloading() {
        return view != null && view.isDownloading();
    }

    void setOpacity(float opacity) {
        optionsPanel.setOpacity(opacity);
    }

    void setAutoContrast(boolean b) {
        autoContrast = b;
    }

    GLImage getGLImage() {
        return glImage;
    }

    View getView() {
        return view;
    }

    public APIRequest getAPIRequest() {
        return view == null ? null : view.getAPIRequest();
    }

}
