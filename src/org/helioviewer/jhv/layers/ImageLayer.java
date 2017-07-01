package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.geom.Rectangle2D;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageDataHandler;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.LoadRemoteTask;
import org.helioviewer.jhv.math.IcoSphere;
import org.helioviewer.jhv.math.Mat4;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.opengl.GLImage.DifferenceMode;
import org.helioviewer.jhv.opengl.GLSLShader;
import org.helioviewer.jhv.opengl.GLSLSolarShader;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.VBO;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.view.View;
import org.json.JSONObject;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class ImageLayer extends AbstractRenderable implements ImageDataHandler {

    private final GLImage glImage = new GLImage();
    private final VBO positionVBO = VBO.gen_float_VBO(GLSLSolarShader.positionRef, 3);
    private final VBO indexVBO = VBO.gen_index_VBO();
    private final ImageLayerOptions optionsPanel;

    private LoadRemoteTask worker;
    private View view;

    private static final String loading = "Loading...";

    public static ImageLayer create(JSONObject jo) {
        ImageLayer imageLayer = new ImageLayer(jo);
        ImageViewerGui.getRenderableContainer().addBeforeRenderable(imageLayer);
        return imageLayer;
    }

    @Override
    public void serialize(JSONObject jo) {
        APIRequest apiRequest = getAPIRequest();
        if (apiRequest != null) {
            jo.put("APIRequest", apiRequest.toJson());
            jo.put("imageParams", glImage.toJson());
        }
    }

    private ImageLayer(JSONObject jo) {
        if (jo != null) {
            JSONObject apiRequest = jo.optJSONObject("APIRequest");
            if (apiRequest != null) {
                load(APIRequest.fromJson(apiRequest));

                JSONObject imageParams = jo.optJSONObject("imageParams");
                if (imageParams != null)
                    glImage.fromJson(imageParams);
            }
        }
        optionsPanel = new ImageLayerOptions(this);
    }

    public void load(APIRequest req) {
        if (req.equals(getAPIRequest()))
            return;

        if (worker != null)
            worker.cancel(true);
        worker = new LoadRemoteTask(this, req, 0);
        JHVGlobals.getExecutorService().execute(worker);
    }

    public void unload() {
        if (view == null) // not changing view
            ImageViewerGui.getRenderableContainer().removeRenderable(this);
        else {
            worker = null;
            Displayer.display();
        }
    }

    @Override
    public void init(GL2 gl) {
        glImage.init(gl);

        FloatBuffer positionBuffer = IcoSphere.IcoSphere.a;
        IntBuffer indexBuffer = IcoSphere.IcoSphere.b;

        positionVBO.init(gl);
        positionVBO.bindBufferData(gl, positionBuffer, Buffers.SIZEOF_FLOAT);

        indexVBO.init(gl);
        indexVBO.bindBufferData(gl, indexBuffer, Buffers.SIZEOF_INT);
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);

        if (Displayer.multiview) {
            ImageViewerGui.getRenderableContainer().arrangeMultiView(true);
        }
    }

    public void setView(View _view) {
        if (view != null)
            unsetView();

        view = _view;
        worker = null; // drop reference

        setEnabled(true); // enable optionsPanel
        ImageViewerGui.getRenderableContainerPanel().setOptionsPanel(this);

        view.setImageLayer(this);
        view.setDataHandler(this);
        Layers.addLayer(view);
        ImageViewerGui.getRenderableContainer().refreshTable();

        if (Displayer.multiview) {
            ImageViewerGui.getRenderableContainer().arrangeMultiView(true);
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
            ImageViewerGui.getRenderableContainer().arrangeMultiView(true);
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

    private static final double[] depth = { 1., 1., 0., 1. };
    private static final double[] depthMini = { 0., 0., 0., 0. };
    private static final double[] depthScale = { 1., 1., 1., 1. };

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

    private static Mat4 getOrthoMatrixInverse(double fov, double aspect, double distance) {
        double width = distance * Math.tan(0.5 * fov);
        return Mat4.orthoInverse(-width * aspect, width * aspect, -width, width, 0, 0); // do clipping planes matter?
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
            int numLayers = imageData.getMetaData().getInnerCutOffRadius() > 1 ? 1 : Layers.getNumEnabledLayers(); // should be two groups
            glImage.applyFilters(gl, imageData, prevImageData, baseImageData, shader, numLayers);
            shader.bindViewport(gl, vp.x, vp.yGL, vp.width, vp.height);

            Position.Q viewpoint = imageData.getViewpoint();

            Mat4 vpmi = getOrthoMatrixInverse(camera.getFOV(), vp.aspect, viewpoint.distance); // use current FOV
            if (Displayer.mode == Displayer.DisplayMode.Orthographic)
                vpmi.translate(-camera.getCurrentTranslation().x, -camera.getCurrentTranslation().y, 0.);
            else
                vpmi.translate(-camera.getCurrentTranslation().x / vp.aspect, -camera.getCurrentTranslation().y, 0.);
            shader.bindMatrix(gl, vpmi.getFloatArray());

            Quat q = Quat.rotate(camera.getCurrentDragRotation(), viewpoint.orientation);
            shader.bindCameraDifferenceRotationQuat(gl, Quat.rotateWithConjugate(q, imageData.getMetaData().getCenterRotation()));
            DifferenceMode diffMode = glImage.getDifferenceMode();
            if (diffMode == DifferenceMode.Base) {
                shader.bindDiffCameraDifferenceRotationQuat(gl, Quat.rotateWithConjugate(q, baseImageData.getMetaData().getCenterRotation()));
            } else if (diffMode == DifferenceMode.Running) {
                shader.bindDiffCameraDifferenceRotationQuat(gl, Quat.rotateWithConjugate(q, prevImageData.getMetaData().getCenterRotation()));
            }

            shader.bindPolarRadii(gl, scale.getYstart(), scale.getYstop());

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

    public boolean isLoadedForState() {
        if (view == null)
            return worker == null;
        return view.getFrameCacheStatus(view.getMaximumFrameNumber()) != null;
    }

}
