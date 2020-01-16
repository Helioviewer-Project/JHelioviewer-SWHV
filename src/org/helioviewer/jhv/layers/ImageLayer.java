package org.helioviewer.jhv.layers;

import java.awt.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageDataHandler;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DownloadViewTask;
import org.helioviewer.jhv.io.LoadRemoteTask;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.opengl.GLImage.DifferenceMode;
import org.helioviewer.jhv.opengl.GLListener;
import org.helioviewer.jhv.opengl.GLSLSolarShader;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.view.BaseView;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.View;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class ImageLayer extends AbstractLayer implements ImageDataHandler {

    private final GLImage glImage = new GLImage();
    private final DecodeExecutor executor = new DecodeExecutor();
    private final ImageLayerOptions optionsPanel = new ImageLayerOptions(this);

    private boolean removed;
    private LoadRemoteTask worker;
    private View view = new BaseView(null, null, null);

    public static ImageLayer create(JSONObject jo) {
        ImageLayer imageLayer = new ImageLayer(jo);
        JHVFrame.getLayers().add(imageLayer);
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
    }

    public void load(APIRequest req) {
        if (req.equals(getAPIRequest()))
            return;

        if (worker != null)
            worker.cancel(true);
        worker = new LoadRemoteTask(this, req);
        JHVGlobals.getExecutorService().execute(worker);
        JHVFrame.getLayersPanel().refresh(); // give feedback asap
    }

    public void unload() {
        if (view.getURI() == null)
            JHVFrame.getLayers().remove(this);
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
    }

    @Override
    public void init(GL2 gl) {
        glImage.init(gl);
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
        JHVFrame.getLayersPanel().setOptionsPanel(this);

        view.setDataHandler(this);
        CameraHelper.zoomToFit(Display.getMiniCamera());
        Layers.setActiveImageLayer(this);
        Movie.setFrame(0); //!

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
        executor.abolish();
        unsetView();
        if (Display.multiview) {
            ImageLayers.arrangeMultiView(true);
        }
        dispose(gl);
        System.gc(); // reclaim memory asap
        removed = true;
    }

    @Override
    public void prerender(GL2 gl) {
        if (imageData == null) {
            return;
        }
        glImage.streamImage(gl, imageData, prevImageData, baseImageData);
    }

    @Override
    public void renderMiniview(Camera camera, Viewport vp, GL2 gl) {
        render(camera, vp, gl);
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        render(camera, vp, gl);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (imageData == null) {
            return;
        }
        if (!isVisible[vp.idx])
            return;

        GLSLSolarShader shader = Display.mode.shader;
        shader.use(gl);

        shader.bindPolarRadii(gl, Display.mode.scale.getYstart(), Display.mode.scale.getYstop()); // independent
        shader.bindMatrix(gl, camera.getTransformationInverse(vp.aspect)); // viewport dependent
        shader.bindViewport(gl, vp.x, vp.yGL, vp.width, vp.height); // viewport dependent

        glImage.applyFilters(gl, imageData, prevImageData, baseImageData, shader);

        Quat q = Quat.rotate(camera.getCurrentDragRotation(), imageData.getViewpoint().toQuat()); // sync with camera at decode command moment
        shader.bindCameraDifferenceRotationQuat(gl, Quat.rotateWithConjugate(q, imageData.getMetaData().getCenterRotation()));

        DifferenceMode diffMode = glImage.getDifferenceMode();
        if (diffMode == DifferenceMode.Base) {
            shader.bindDiffCameraDifferenceRotationQuat(gl, Quat.rotateWithConjugate(q, baseImageData.getMetaData().getCenterRotation()));
        } else if (diffMode == DifferenceMode.Running) {
            shader.bindDiffCameraDifferenceRotationQuat(gl, Quat.rotateWithConjugate(q, prevImageData.getMetaData().getCenterRotation()));
        }
        GLListener.glslSolar.render(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return imageData == null ? "Loading..." : imageData.getMetaData().getDisplayName();
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
        glImage.dispose(gl);
    }

    private ImageData imageData;
    private ImageData prevImageData;
    private ImageData baseImageData;

    private void setImageData(@Nonnull ImageData newImageData) {
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

    @Nullable
    public ImageData getImageData() {
        return imageData;
    }

    @Nonnull
    public MetaData getMetaData() { //!
        return imageData == null ? view.getMetaData(new JHVDate(0)) : imageData.getMetaData();
    }

    @Override
    public void handleData(ImageData newImageData) {
        setImageData(newImageData);
        JHVFrame.getLayers().fireTimeUpdated(this);
        ImageLayers.displaySynced(imageData.getViewpoint());
    }

    @Override
    public boolean isDownloading() {
        return worker != null || view.isDownloading();
    }

    @Override
    public boolean isLocal() {
        return view.isLocal();
    }

    @Nonnull
    public GLImage getGLImage() {
        return glImage;
    }

    @Nonnull
    public DecodeExecutor getExecutor() {
        return executor;
    }

    @Nonnull
    public View getView() {
        return view;
    }

    @Nullable
    public APIRequest getAPIRequest() {
        return view.getAPIRequest();
    }

    long getStartTime() {
        if (worker != null) // don't use for timespan calculation
            return Long.MAX_VALUE;
        APIRequest req = getAPIRequest(); // for locked timelines
        return req == null ? view.getFirstTime().milli : req.startTime;
    }

    long getEndTime() {
        if (worker != null) // don't use for timespan calculation
            return Long.MIN_VALUE;
        APIRequest req = getAPIRequest(); // for locked timelines
        return req == null ? view.getLastTime().milli : req.endTime;
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
