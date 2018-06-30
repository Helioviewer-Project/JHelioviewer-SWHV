package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageDataHandler;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DownloadViewTask;
import org.helioviewer.jhv.io.LoadRemoteTask;
import org.helioviewer.jhv.math.IcoSphere;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.opengl.GLImage.DifferenceMode;
import org.helioviewer.jhv.opengl.GLSLShader;
import org.helioviewer.jhv.opengl.GLSLSolarShader;
import org.helioviewer.jhv.opengl.VBO;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.view.AbstractView;
import org.helioviewer.jhv.view.View;
import org.json.JSONObject;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;

public class ImageLayer extends AbstractLayer implements ImageDataHandler {

    private final GLImage glImage = new GLImage();
    private final VBO positionVBO = VBO.gen_float_VBO(0, 4);
    private final VBO indexVBO = VBO.gen_index_VBO();
    private final ImageLayerOptions optionsPanel;

    private boolean removed;
    private LoadRemoteTask worker;
    private View view = new AbstractView(null, null);

    public static ImageLayer create(JSONObject jo) {
        ImageLayer imageLayer = new ImageLayer(jo);
        ImageViewerGui.getLayers().add(imageLayer);
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
        worker = new LoadRemoteTask(this, req);
        JHVGlobals.getExecutorService().execute(worker);
        ImageViewerGui.getLayersPanel().refresh(); // give feedback asap
    }

    public void unload() {
        if (view.getURI() == null)
            ImageViewerGui.getLayers().remove(this);
        if (worker != null) {
            worker.cancel(true);
            worker = null;
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
        if (Display.multiview) {
            ImageLayers.arrangeMultiView(true);
        }
    }

    public void setView(View _view) {
        if (removed) //!
            return;

        unsetView();
        view = _view;
        worker = null; // drop reference

        optionsPanel.getRunningDifferencePanel().downloadVisible(!isLocal());
        setEnabled(true); // enable optionsPanel
        ImageViewerGui.getLayersPanel().setOptionsPanel(this);

        view.setDataHandler(this);
        CameraHelper.zoomToFit(Display.getMiniCamera());
        Layers.setActiveImageLayer(this);
        Movie.setFrame(0); //!
        Movie.timespanChanged();

        if (Display.multiview) {
            ImageLayers.arrangeMultiView(true);
        }
        optionsPanel.setLUT(view.getDefaultLUT());
    }

    private void unsetView() {
        stopDownloadView();

        CameraHelper.zoomToFit(Display.getMiniCamera());
        view.setDataHandler(null);
        view.abolish();

        imageData = prevImageData = baseImageData = null;
    }

    @Override
    public void remove(GL2 gl) {
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        unsetView();
        if (Display.multiview) {
            ImageLayers.arrangeMultiView(true);
        }
        dispose(gl);
        removed = true;
    }

    @Override
    public void prerender(GL2 gl) {
        if (imageData == null) {
            return;
        }
        glImage.streamImage(gl, imageData, prevImageData, baseImageData);
    }

    private static final double[] depth = { 1., 1., 0., 1. };
    private static final double[] depthScale = { 1., 1., 1., 1. };

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        _render(camera, vp, gl, depth);
    }

    @Override
    public void renderMiniview(Camera camera, Viewport vp, GL2 gl) {
        _render(camera, vp, gl, depth);
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

        GLSLSolarShader shader = Display.mode.shader;
        GridScale scale = Display.mode.scale;

        shader.bind(gl);
        {
            glImage.applyFilters(gl, imageData, prevImageData, baseImageData, shader);
            shader.bindViewport(gl, vp.x, vp.yGL, vp.width, vp.height);
            shader.bindMatrix(gl, camera.getTransformationInverse(vp.aspect));

            Quat q = Quat.rotate(camera.getCurrentDragRotation(), imageData.getViewpoint().toQuat());
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
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return worker != null ? "Loading..." : view.getName();
    }

    @Nullable
    @Override
    public String getTimeString() {
        return imageData == null ? null : imageData.getMetaData().getViewpoint().time.toString();
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

    public MetaData getMetaData() { //!
        return imageData == null ? view.getMetaData(new JHVDate(0)) : imageData.getMetaData();
    }

    @Override
    public void handleData(ImageData newImageData) {
        setImageData(newImageData);
        ImageViewerGui.getLayers().fireTimeUpdated(this);
        Display.handleData(imageData.getViewpoint().time.milli);
    }

    @Override
    public boolean isDownloading() {
        return worker != null || view.isDownloading();
    }

    @Override
    public boolean isLocal() {
        return view.isLocal();
    }

    public GLImage getGLImage() {
        return glImage;
    }

    public View getView() {
        return view;
    }

    public APIRequest getAPIRequest() {
        return view.getAPIRequest();
    }

    public long getStartTime() {
        APIRequest req = getAPIRequest(); // for locked timelines
        return req == null ? view.getFirstTime().milli : req.startTime;
    }

    public long getEndTime() {
        APIRequest req = getAPIRequest(); // for locked timelines
        return req == null ? view.getLastTime().milli : req.endTime;
    }

    public double getAutoBrightness() {
        return imageData == null ? 1 : imageData.getAutoBrightness();
    }

    public boolean isLoadedForState() {
        return worker == null && view.getFrameCacheStatus(view.getMaximumFrameNumber()) != null;
    }

    private DownloadViewTask downloadTask;

    public void startDownloadView() {
        if (downloadTask != null)
            downloadTask.cancel(true);
        downloadTask = new DownloadViewTask(this, view);
        JHVGlobals.getExecutorService().execute(downloadTask);
    }

    public void stopDownloadView() {
        if (downloadTask != null) {
            downloadTask.cancel(true);
            downloadTask = null;
        }
    }

    public void doneDownloadView() {
        optionsPanel.getRunningDifferencePanel().done();
    }

    public void progressDownloadView(int percent) {
        optionsPanel.getRunningDifferencePanel().setValue(percent);
    }

}
