package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.geom.Rectangle2D;
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
import org.helioviewer.jhv.opengl.GLImage.DifferenceMode;
import org.helioviewer.jhv.opengl.GLSLShader;
import org.helioviewer.jhv.opengl.GLSLSolarShader;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.VBO;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ImageDataHandler;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class ImageLayer extends AbstractRenderable implements ImageDataHandler {

    private final GLImage glImage = new GLImage();
    private final ImageLayerOptions optionsPanel;
    private VBO positionVBO;
    private VBO indexVBO;

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

        positionVBO = new VBO(GL2.GL_ARRAY_BUFFER, GLSLSolarShader.positionRef, 3);
        positionVBO.init(gl);
        positionVBO.bindBufferData(gl, positionBuffer, Buffers.SIZEOF_FLOAT);

        indexVBO = new VBO(GL2.GL_ELEMENT_ARRAY_BUFFER, -1, -1);
        indexVBO.init(gl);
        indexVBO.bindBufferData(gl, indexBuffer, Buffers.SIZEOF_INT);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
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
        ImageViewerGui.getRenderableContainerPanel().setOptionsPanel(this);

        view.setImageLayer(this);
        view.setDataHandler(this);
        Layers.addLayer(view);
        ImageViewerGui.getRenderableContainer().refreshTable();

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
            ImageViewerGui.getRenderableContainer().refreshTable();
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

    private static final double[] depth = new double[] { 1., 1., 0., 1. };
    private static final double[] depthMini = new double[] { 0., 0., 0., 0. };
    private static final double[] depthScale = new double[] { 1., 1., 1., 1. };

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        _render(camera, vp, gl, depth);
    }

    @Override
    public void renderMiniview(Camera camera, Viewport vp, GL2 gl) {
        _render(camera, vp, gl, depthMini);
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        _render(camera, vp, gl, depthScale);
    }

    private void _render(Camera camera, Viewport vp, GL2 gl, double[] depthrange) {
        if (imageData == null) {
            return;
        }
        if (!isVisible[vp.idx])
            return;

        GLSLSolarShader shader = Displayer.mode.shader;
        GridScale scale = Displayer.mode.scale;

        shader.bind(gl);
        {
            glImage.applyFilters(gl, imageData, prevImageData, baseImageData, shader);

            shader.setViewport(vp.x, vp.yGL, vp.width, vp.height);
            shader.filter(gl);

            camera.push(imageData.getViewpoint());

            Mat4 vpmi = CameraHelper.getOrthoMatrixInverse(camera, vp);
            if (Displayer.mode == Displayer.DisplayMode.Orthographic)
                vpmi.translate(new Vec3(-camera.getCurrentTranslation().x, -camera.getCurrentTranslation().y, 0.));
            else
                vpmi.translate(new Vec3(-camera.getCurrentTranslation().x / vp.aspect, -camera.getCurrentTranslation().y, 0.));

            Quat q = camera.getRotation();
            shader.bindMatrix(gl, vpmi.getFloatArray());
            shader.bindCameraDifferenceRotationQuat(gl, Quat.rotateWithConjugate(q, imageData.getMetaData().getCenterRotation()));

            DifferenceMode diffMode = glImage.getDifferenceMode();
            if (diffMode == DifferenceMode.BaseRotation) {
                shader.bindDiffCameraDifferenceRotationQuat(gl, Quat.rotateWithConjugate(q, baseImageData.getMetaData().getCenterRotation()));
            } else if (diffMode == DifferenceMode.RunningRotation) {
                shader.bindDiffCameraDifferenceRotationQuat(gl, Quat.rotateWithConjugate(q, prevImageData.getMetaData().getCenterRotation()));
            }
            shader.bindAngles(gl, imageData.getMetaData().getViewpointL());
            shader.setPolarRadii(gl, scale.getYstart(), scale.getYstop());
            camera.pop();

            positionVBO.bindArray(gl);
            indexVBO.bindArray(gl);
            {
                if (shader == GLSLSolarShader.ortho) {
                    shader.bindIsDisc(gl, 1);
                    gl.glDepthRange(depthrange[2], depthrange[3]);
                    gl.glDrawElements(GL2.GL_TRIANGLES, indexVBO.bufferSize - 6, GL2.GL_UNSIGNED_INT, 0);
                    shader.bindIsDisc(gl, 0);
                }
                gl.glDepthRange(depthrange[0], depthrange[1]);
                gl.glDrawElements(GL2.GL_TRIANGLES, 6, GL2.GL_UNSIGNED_INT, (indexVBO.bufferSize - 6) * Buffers.SIZEOF_INT);

                gl.glDepthRange(0, 1);
            }
            indexVBO.unbindArray(gl);
            positionVBO.unbindArray(gl);
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
        positionVBO.dispose(gl);
        indexVBO.dispose(gl);
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

    void setOpacity(float _opacity) { // deliberate, for multiview
        optionsPanel.setOpacity(_opacity);
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

    double getAutoBrightness() {
        return imageData.getAutoBrightness();
    }

}
