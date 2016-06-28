package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.geom.Rectangle2D;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

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
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.io.APIRequestManager.APIRequest;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.opengl.GLSLSolarShader;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.helioviewer.jhv.threads.JHVWorker;
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

    private JHVWorker<?, ?> worker;
    private View view;

    private static final double vpScale = 0.035;
    private static final String loading = "Loading...";

    public ImageLayer() {
        optionsPanel = new ImageLayerOptions(this);
        ComponentUtils.setEnabled(optionsPanel, false);
        setVisible(true);
    }

    public void setWorker(JHVWorker<?, ?> worker) {
        this.worker = worker;
    }

    @Override
    public void init(GL2 gl) {
        glImage.init(gl);

        FloatBuffer positionBuffer = IcoSphere.IcoSphere.a;
        IntBuffer indexBuffer = IcoSphere.IcoSphere.b;

        positionBufferID = generate(gl);
        int positionBufferSize = positionBuffer.capacity();
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

    private boolean isCor(String name) {
        return name.contains("LASCO") || name.contains("COR");
    }

    private float opacity = -1;

    public void setView(View _view) {
        if (view != null)
            unsetView();

        view = _view;
        worker = null; // drop reference

        view.setImageLayer(this);
        view.setDataHandler(this);
        Layers.addLayer(view);
        ImageViewerGui.getRenderableContainer().fireListeners();

        if (Displayer.multiview) {
            ImageViewerGui.getRenderableContainer().arrangeMultiView(true);
        } else if (opacity == -1) { // first time
            if (isCor(view.getName()))
                opacity = 1;
            else {
                int count = 0;
                for (int i = 0; i < Layers.getNumLayers(); i++) {
                    if (!isCor(Layers.getLayer(i).getName()))
                        count++;
                }
                opacity = (float) (1. / count);
            }
            optionsPanel.setOpacity(opacity);
        }
        optionsPanel.setLUT(view.getDefaultLUT());
        ComponentUtils.setEnabled(optionsPanel, true);
    }

    private void unsetView() {
        ComponentUtils.setEnabled(optionsPanel, false);
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
        if (worker != null)
            worker.cancel(true);
        unsetView();
        if (Displayer.multiview) {
            ImageViewerGui.getRenderableContainer().arrangeMultiView(true);
        }
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

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
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

    public void setOpacity(float opacity) {
        optionsPanel.setOpacity(opacity);
    }

    GLImage getGLImage() {
        return glImage;
    }

    View getView() {
        return view;
    }

    public APIRequest getAPIRequest() {
        if (view != null)
            return view.getAPIRequest();
        return null;
    }

}
