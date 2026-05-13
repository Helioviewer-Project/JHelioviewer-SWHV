package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.util.Set;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageFilter;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DownloadLayer;
import org.helioviewer.jhv.io.LoadLayer;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.opengl.GLImage.DifferenceMode;
import org.helioviewer.jhv.opengl.GLSLSolar;
import org.helioviewer.jhv.opengl.GLSLSolarShader;
import org.helioviewer.jhv.view.BaseView;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.wcs.WcsHeader;

import org.json.JSONObject;

public class ImageLayer extends AbstractLayer implements View.DataHandler {

    private final GLImage glImage;
    private final DecodeExecutor executor;
    private final ImageLayerOptions optionsPanel;

    private boolean removed;
    private Future<?> worker;
    protected View view;

    public static ImageLayer create(JSONObject jo) {
        ImageLayer imageLayer = createDetached(jo);
        Layers.add(imageLayer);
        return imageLayer;
    }

    // Only for state restore, which batches layer registration.
    public static ImageLayer createDetached(JSONObject jo) {
        return new ImageLayer(jo);
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
        if (removed)
            return;
        if (req.equals(getAPIRequest()))
            return;

        cancelLoadTask();
        worker = LoadLayer.submit(this, req);
        Layers.fireLayerUpdated(this); // give feedback asap
    }

    public void unload() {
        if (view.getBaseName() == null)
            Layers.remove(this);
        cancelLoadTask();
    }

    @Override
    public void init() {
        glImage.init();
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

        replaceView(_view);
        activateView();
    }

    private void replaceView(View newView) {
        ImageFilter.Type filterType = view.getFilter();
        unsetView();
        view = newView;
        worker = null; // drop reference
        view.setFilter(filterType);
        view.setDataHandler(this);
    }

    private void activateView() {
        optionsPanel.downloadVisible(!isLocal());
        setEnabled(true); // enable optionsPanel

        CameraHelper.zoomToFit(Display.getMiniCamera());
        Layers.setActiveImageLayer(this);

        if (Display.multiview) {
            ImageLayers.arrangeMultiView(true);
        }
        optionsPanel.setLUT(view.getDefaultLUT());
        Layers.fireLayerUpdated(this);
    }

    private void unsetView() {
        cancelDownloadTask();

        CameraHelper.zoomToFit(Display.getMiniCamera());
        view.setDataHandler(null);
        view.abolish();

        imageData = prevImageData = baseImageData = null;
    }

    @Override
    public void remove() {
        removed = true;
        cancelAsyncTasks();
        executor.abolish();
        unsetView();
        if (Display.multiview) {
            ImageLayers.arrangeMultiView(true);
        }
        dispose();
        //System.gc(); // reclaim memory asap
    }

    @Override
    public void prerender() {
        if (imageData == null) {
            return;
        }
        glImage.streamImage(imageData, prevImageData, baseImageData);
    }

    @Override
    public void renderMiniview(Camera camera, Viewport vp) {
        render(camera, vp);
    }

    @Override
    public void renderScale(Camera camera, Viewport vp) {
        render(camera, vp);
    }

    private final float[] crval0 = new float[2];
    private final float[] crval1 = new float[2];
    private final float[] latiGrid0 = new float[3];
    private final float[] latiGrid1 = new float[3];

    @Override
    public void render(Camera camera, Viewport vp) {
        if (imageData == null) {
            return;
        }
        if (!isVisible[vp.idx])
            return;

        GLSLSolarShader shader = Display.mode.shader;
        shader.use();
        glImage.applyFilters();

        Position cameraViewpoint = imageData.getViewpoint(); // camera at decode command moment
        Quat q = Quat.rotate(camera.getDragRotation(), cameraViewpoint.toQuat());

        MetaData meta0 = imageData.getMetaData();
        Position metaViewpoint0 = meta0.getViewpoint();
        ImageData imageDataDiff = glImage.getDifferenceMode() == DifferenceMode.Base ? baseImageData : prevImageData;
        MetaData meta1 = imageDataDiff.getMetaData();
        Position metaViewpoint1 = meta1.getViewpoint();
        WcsHeader wcs0 = meta0.getWcsHeader();
        WcsHeader wcs1 = meta1.getWcsHeader();

        Quat cameraDiff0 = Quat.rotateWithConjugate(q, metaViewpoint0.toQuat());
        Quat cameraDiff1 = Quat.rotateWithConjugate(q, metaViewpoint1.toQuat());

        Quat crota0 = wcs0.crota;
        Quat crota1 = wcs1.crota;
        double deltaCROTA = glImage.getDeltaCROTA();
        if (deltaCROTA != 0) {
            Quat dquat = Quat.createAxisZ(Math.toRadians(deltaCROTA));
            crota0 = Quat.rotate(dquat, crota0);
            crota1 = Quat.rotate(dquat, crota1);
        }

        int deltaCRVAL1 = glImage.getDeltaCRVAL1();
        if (deltaCRVAL1 == 0) {
            crval0[0] = (float) wcs0.crval.x;
            crval1[0] = (float) wcs1.crval.x;
        } else {
            crval0[0] = (float) (wcs0.crval.x + deltaCRVAL1 * meta0.getUnitPerArcsec());
            crval1[0] = (float) (wcs1.crval.x + deltaCRVAL1 * meta1.getUnitPerArcsec());
        }

        int deltaCRVAL2 = glImage.getDeltaCRVAL2();
        if (deltaCRVAL2 == 0) {
            crval0[1] = (float) wcs0.crval.y;
            crval1[1] = (float) wcs1.crval.y;
        } else {
            crval0[1] = (float) (wcs0.crval.y + deltaCRVAL2 * meta0.getUnitPerArcsec());
            crval1[1] = (float) (wcs1.crval.y + deltaCRVAL2 * meta1.getUnitPerArcsec());
        }

        float deltaT0 = 0, deltaT1 = 0;
        if (ImageLayers.getDiffRotationMode()) {
            deltaT0 = (float) ((cameraViewpoint.time.milli - metaViewpoint0.time.milli) * 1e-9);
            deltaT1 = (float) ((cameraViewpoint.time.milli - metaViewpoint1.time.milli) * 1e-9);
        }

        GLSLSolarShader.bindWCS(
                cameraDiff0, imageData.getRegion(), crota0, crval0, deltaT0,
                cameraDiff1, imageDataDiff.getRegion(), crota1, crval1, deltaT1);
        shader.bindPV(wcs0.pv2, wcs1.pv2);

        Quat sourceView0 = wcs0.projection.isSurfaceMap() ? q : metaViewpoint0.toQuat();
        Quat sourceView1 = wcs1.projection.isSurfaceMap() ? q : metaViewpoint1.toQuat();
        Quat displayMap0 = Quat.ZERO;
        Quat displayMap1 = Quat.ZERO;
        if (Display.mode.isLatitudinal()) {
            displayMap0 = displayMap1 = Display.gridType.mapRotation(cameraViewpoint);
            GridType gridType = Display.gridType;
            latiGrid0[0] = (float) latiLongitude(gridType, cameraViewpoint, metaViewpoint0);
            latiGrid0[1] = (float) gridType.toLatitude(metaViewpoint0);
            latiGrid0[2] = (float) metaViewpoint0.lat;
            latiGrid1[0] = (float) latiLongitude(gridType, cameraViewpoint, metaViewpoint1);
            latiGrid1[1] = (float) gridType.toLatitude(metaViewpoint1);
            latiGrid1[2] = (float) metaViewpoint1.lat;
        }
        shader.bindLatiGrid(latiGrid0, latiGrid1);

        GLSLSolarShader.bindProjection(
                wcs0.projection, (float) wcs0.unitsPerRad, (float) metaViewpoint0.distance, sourceView0, displayMap0,
                wcs1.projection, (float) wcs1.unitsPerRad, (float) metaViewpoint1.distance, sourceView1, displayMap1);

        GLSLSolar.quad.render();
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
    public void dispose() {
        glImage.dispose();
    }

    private ImageData imageData;
    private ImageData prevImageData;
    private ImageData baseImageData;

    private void setImageData(@Nonnull ImageData newImageData) {
        long newMilli = newImageData.getMetaData().getViewpoint().time.milli;
        if (baseImageData == null || newMilli == view.getFirstTime().milli) {
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

    void collectImageBuffers(Set<ImageBuffer> retained) {
        if (imageData != null)
            retained.add(imageData.getImageBuffer());
        if (prevImageData != null)
            retained.add(prevImageData.getImageBuffer());
        if (baseImageData != null)
            retained.add(baseImageData.getImageBuffer());
        if (glImage != null)
            glImage.collectImageBuffers(retained);
    }

    @Nonnull
    public MetaData getMetaData() { //!
        return imageData == null ? view.getMetaData(view.getFirstTime()) : imageData.getMetaData();
    }

    @Override
    public void handleData(ImageData newImageData) {
        if (removed)
            return;
        newImageData.getImageBuffer().allowExplicitFree();
        setImageData(newImageData);
        Layers.fireTimeUpdated(this);
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

    public boolean isLoadingForTimespan() {
        return worker != null;
    }

    public long getStartTime() {
        APIRequest req = getAPIRequest(); // for locked timelines
        return req == null ? view.getFirstTime().milli : req.startTime();
    }

    public long getEndTime() {
        APIRequest req = getAPIRequest(); // for locked timelines
        return req == null ? view.getLastTime().milli : req.endTime();
    }

    public boolean isLoadedForState() {
        return worker == null && view.getFrameCompletion(view.getMaximumFrameNumber()) != null;
    }

    private void cancelAsyncTasks() {
        cancelLoadTask();
        cancelDownloadTask();
    }

    private void cancelLoadTask() {
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
    }

    private Future<?> downloadTask;

    void cancelDownloadTask() {
        if (downloadTask != null) {
            downloadTask.cancel(true);
            downloadTask = null;
        }
    }

    public void startDownload(DownloadLayer.Progress progress) {
        cancelDownloadTask();
        APIRequest req = view.getAPIRequest();
        if (req != null && view.getBaseName() != null) // should not happen
            downloadTask = DownloadLayer.submit(req, this, view.getBaseName(), progress);
    }

}
