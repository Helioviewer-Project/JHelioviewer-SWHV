package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageFilter;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DownloadLayer;
import org.helioviewer.jhv.io.LoadLayer;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.opengl.GLImage.DifferenceMode;
import org.helioviewer.jhv.opengl.GLListener;
import org.helioviewer.jhv.opengl.GLSLSolarShader;
import org.helioviewer.jhv.view.BaseView;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.View;
import org.json.JSONObject;

import com.jogamp.opengl.GL3;

public class ImageLayer extends AbstractLayer implements ImageData.Handler {

    private final GLImage glImage;
    private final DecodeExecutor executor;
    private final ImageLayerOptions optionsPanel;

    private boolean removed;
    private Future<?> worker;
    protected View view;

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

    // Constructor for NullImageLayer
    protected ImageLayer(View _view) {
        view = _view;
        glImage = null;
        executor = null;
        optionsPanel = null;
    }

    private ImageLayer(JSONObject jo) {
        try {
            view = new BaseView(null, null);
        } catch (Exception e) { // impossible
            e.printStackTrace();
        }

        glImage = new GLImage();
        executor = new DecodeExecutor();

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
        worker = LoadLayer.submit(this, req);
        JHVFrame.getLayersPanel().refresh(); // give feedback asap
    }

    public void unload() {
        if (view.getBaseName() == null)
            JHVFrame.getLayers().remove(this);
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
    }

    @Override
    public void init(GL3 gl) {
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

        ImageFilter.Type filterType = view.getFilter();
        unsetView();
        view = _view;
        worker = null; // drop reference
        view.setFilter(filterType);

        optionsPanel.downloadVisible(!isLocal());
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
        stopDownload();

        CameraHelper.zoomToFit(Display.getMiniCamera());
        view.setDataHandler(null);
        view.abolish();

        imageData = prevImageData = baseImageData = null;
    }

    @Override
    public void remove(GL3 gl) {
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
        //System.gc(); // reclaim memory asap
        removed = true;
    }

    @Override
    public void prerender(GL3 gl) {
        if (imageData == null) {
            return;
        }
        glImage.streamImage(gl, imageData, prevImageData, baseImageData);
    }

    @Override
    public void renderMiniview(Camera camera, Viewport vp, GL3 gl) {
        render(camera, vp, gl);
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL3 gl) {
        render(camera, vp, gl);
    }

    private final float[] crval0 = new float[2];
    private final float[] crval1 = new float[2];

    @Override
    public void render(Camera camera, Viewport vp, GL3 gl) {
        if (imageData == null) {
            return;
        }
        if (!isVisible[vp.idx])
            return;

        GLSLSolarShader shader = Display.mode.shader;
        shader.use(gl);

        MetaData meta0 = imageData.getMetaData();
        glImage.applyFilters(gl, meta0, imageData);

        Position cameraViewpoint = imageData.getViewpoint(); // camera at decode command moment
        Quat q = Quat.rotate(camera.getDragRotation(), cameraViewpoint.toQuat());

        Position metaViewpoint0 = meta0.getViewpoint();
        ImageData imageDataDiff = glImage.getDifferenceMode() == DifferenceMode.Base ? baseImageData : prevImageData;
        MetaData meta1 = imageDataDiff.getMetaData();
        Position metaViewpoint1 = meta1.getViewpoint();

        Quat cameraDiff0 = Quat.rotateWithConjugate(q, metaViewpoint0.toQuat());
        Quat cameraDiff1 = Quat.rotateWithConjugate(q, metaViewpoint1.toQuat());

        Quat crota0 = meta0.getCROTA();
        Quat crota1 = meta1.getCROTA();
        double deltaCROTA = glImage.getDeltaCROTA();
        if (deltaCROTA != 0) {
            Quat dquat = Quat.createAxisZ(Math.toRadians(deltaCROTA));
            crota0 = Quat.rotate(dquat, crota0);
            crota1 = Quat.rotate(dquat, crota1);
        }

        int deltaCRVAL1 = glImage.getDeltaCRVAL1();
        if (deltaCRVAL1 == 0) {
            crval0[0] = (float) meta0.getCRVAL().x;
            crval1[0] = (float) meta1.getCRVAL().x;
        } else {
            crval0[0] = (float) (meta0.getCRVAL().x + deltaCRVAL1 * meta0.getUnitPerArcsec());
            crval1[0] = (float) (meta1.getCRVAL().x + deltaCRVAL1 * meta1.getUnitPerArcsec());
        }

        int deltaCRVAL2 = glImage.getDeltaCRVAL2();
        if (deltaCRVAL2 == 0) {
            crval0[1] = (float) meta0.getCRVAL().y;
            crval1[1] = (float) meta1.getCRVAL().y;
        } else {
            crval0[1] = (float) (meta0.getCRVAL().y + deltaCRVAL2 * meta0.getUnitPerArcsec());
            crval1[1] = (float) (meta1.getCRVAL().y + deltaCRVAL2 * meta1.getUnitPerArcsec());
        }

        float deltaT0 = 0, deltaT1 = 0;
        if (ImageLayers.getDiffRotationMode()) {
            deltaT0 = (float) ((cameraViewpoint.time.milli - metaViewpoint0.time.milli) * 1e-9);
            deltaT1 = (float) ((cameraViewpoint.time.milli - metaViewpoint1.time.milli) * 1e-9);
        }

        GLSLSolarShader.bindWCS(gl,
                cameraDiff0, imageData.getRegion(), crota0, crval0, deltaT0,
                cameraDiff1, imageDataDiff.getRegion(), crota1, crval1, deltaT1);
        shader.bindPV(gl, meta0.getPV2(), meta1.getPV2());

        if (Display.mode == Display.ProjectionMode.Latitudinal) {
            GridType gridType = Display.gridType;
            shader.bindAnglesLatiGrid(gl,
                    latiLongitude(gridType, cameraViewpoint, metaViewpoint0),
                    gridType.toLatitude(metaViewpoint0),
                    metaViewpoint0.lat,
                    latiLongitude(gridType, cameraViewpoint, metaViewpoint1),
                    gridType.toLatitude(metaViewpoint1),
                    metaViewpoint1.lat);
        }

        GLListener.glslSolar.render(gl);
    }

    private static double latiLongitude(GridType gridType, Position cameraViewpoint, Position metaViewpoint) {
        double gridLon = gridType.toLongitude(metaViewpoint);
        double lon = gridType == GridType.Viewpoint ? gridLon - cameraViewpoint.lon : metaViewpoint.lon - gridLon;
        return (lon + 3. * Math.PI) % (2. * Math.PI); // centered
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
    public void dispose(GL3 gl) {
        glImage.dispose(gl);
    }

    private ImageData imageData;
    private ImageData prevImageData;
    private ImageData baseImageData;

    private void setImageData(@Nonnull ImageData newImageData) {
        long newMilli = newImageData.getMetaData().getViewpoint().time.milli;
        if (newMilli == view.getFirstTime().milli) {
            baseImageData = newImageData;
        }

        if (imageData == null || baseImageData == newImageData) { // first or loop playback
            prevImageData = newImageData;
        } else if (newMilli != imageData.getMetaData().getViewpoint().time.milli) { // new frame
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
        return imageData == null ? view.getMetaData(view.getFirstTime()) : imageData.getMetaData();
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
        return getAPIRequest() == null;
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

    public long getStartTime() {
        if (worker != null) // don't use for timespan calculation
            return Long.MAX_VALUE;
        APIRequest req = getAPIRequest(); // for locked timelines
        return req == null ? view.getFirstTime().milli : req.startTime();
    }

    public long getEndTime() {
        if (worker != null) // don't use for timespan calculation
            return Long.MIN_VALUE;
        APIRequest req = getAPIRequest(); // for locked timelines
        return req == null ? view.getLastTime().milli : req.endTime();
    }

    public boolean isLoadedForState() {
        return worker == null && view.getFrameCompletion(view.getMaximumFrameNumber()) != null;
    }

    private Future<?> downloadTask;

    public void startDownload() {
        if (downloadTask != null)
            downloadTask.cancel(true);
        APIRequest req = view.getAPIRequest();
        if (req != null && view.getBaseName() != null) // should not happen
            downloadTask = DownloadLayer.submit(req, this, view.getBaseName());
    }

    public void stopDownload() {
        if (downloadTask != null) {
            downloadTask.cancel(true);
            downloadTask = null;
        }
    }

    public void doneDownload() {
        optionsPanel.downloadDone();
    }

    public void progressDownload(int percent) {
        optionsPanel.downloadProgress(percent);
    }

}
