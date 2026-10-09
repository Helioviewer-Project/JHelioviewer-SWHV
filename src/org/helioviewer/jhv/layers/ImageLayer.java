package org.helioviewer.jhv.layers;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageDisplaySettings;
import org.helioviewer.jhv.image.ImageDisplaySettings.DifferenceMode;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DownloadLayer;
import org.helioviewer.jhv.math.Mat2;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.opengl.GLSLImage;
import org.helioviewer.jhv.opengl.GLSLImageShader;
import org.helioviewer.jhv.view.BaseView;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.wcs.WcsHeader;

import org.json.JSONObject;

public class ImageLayer extends AbstractLayer implements View.DataHandler {

    private final ImageDisplaySettings displaySettings = new ImageDisplaySettings();
    private final ImageProcessingSettings processingSettings = new ImageProcessingSettings(this::refreshImage);
    private final GLSLImage glImage = new GLSLImage(displaySettings);
    private final ImageLayerLoader loader;

    private boolean removed;
    protected View view;

    public static ImageLayer create() {
        ImageLayer imageLayer = new ImageLayer();
        Layers.add(imageLayer);
        return imageLayer;
    }

    // Only for state restore, which batches layer registration.
    public static ImageLayer createDetached(JSONObject jo) {
        ImageLayer imageLayer = new ImageLayer();
        imageLayer.applyImageParams(jo.optJSONObject("imageParams"));
        imageLayer.load(APIRequest.fromJson(jo.getJSONObject("APIRequest")));
        return imageLayer;
    }

    @Override
    public void serialize(JSONObject jo) {
        APIRequest apiRequest = view.getAPIRequest();
        if (apiRequest != null) {
            jo.put("APIRequest", apiRequest.toJson());
            JSONObject imageParams = displaySettings.toJson();
            processingSettings.serialize(imageParams);
            jo.put("imageParams", imageParams);
        }
    }

    // Constructor for NullImageLayer
    protected ImageLayer(View _view) {
        view = _view;
        loader = new ImageLayerLoader(processingSettings, v -> {}, () -> {});
    }

    private ImageLayer() {
        view = new BaseView(null, null, processingSettings);

        loader = new ImageLayerLoader(processingSettings, this::setView, this::loadFailed);
    }

    public void applyImageParams(@Nullable JSONObject imageParams) {
        if (imageParams != null) {
            displaySettings.fromJson(imageParams);
            processingSettings.fromJson(imageParams);
        }
    }

    public ImageFilter.Type getFilter() {
        return processingSettings.getFilter();
    }

    public void setFilter(ImageFilter.Type type) {
        processingSettings.setFilter(type);
    }

    void decode(Position viewpoint, double pixFactor) {
        view.decode(viewpoint, pixFactor, processingSettings.fitsParameters().clipRange(view.getClipSet()));
    }

    public ImageProcessingSettings getProcessingSettings() {
        return processingSettings;
    }

    private void refreshImage() {
        if (removed)
            return;
        view.clearCache();
        clearImageData();
        DisplayController.render();
    }

    public void load(APIRequest req) {
        if (removed)
            return;
        if (req.equals(view.getAPIRequest())) {
            loader.cancelLoad();
            Layers.fireLayerUpdated(this);
            return;
        }

        loader.load(req);
        Layers.fireLayerUpdated(this); // give feedback asap
    }

    public void load(List<URI> uris) {
        if (removed)
            return;

        loader.load(uris);
        Layers.fireLayerUpdated(this); // give feedback asap
    }

    private void loadFailed() {
        if (view.getClass() == BaseView.class)
            Layers.remove(this);
        else
            Layers.fireLayerUpdated(this);
    }

    @Override
    public void init() {
        glImage.init();
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);
        ImageLayers.arrangeMultiView();
    }

    void setView(View _view) {
        // Only the initial placeholder is a bare BaseView.
        boolean firstLoad = view.getClass() == BaseView.class;
        loader.cancelDownload();
        unsetView();
        view = _view;
        view.setDataHandler(this);
        displaySettings.setLUT(view.getDefaultLUT(), displaySettings.getInvertLUT());

        if (firstLoad)
            setEnabled(true);

        DisplayController.zoomMiniToFit();
        if (firstLoad || Layers.getActiveImageLayer() == this) {
            Layers.setActiveImageLayer(this);
        } else {
            view.setNearestFrame(Player.getTime());
            DisplayController.render();
        }
        Layers.fireLayerUpdated(this);
    }

    private void unsetView() {
        DisplayController.zoomMiniToFit();
        view.setDataHandler(null);
        view.abolish();

        clearImageData();
    }

    // Release the CPU-side owners now; GL disposal still needs the render context.
    void detachView() {
        removed = true;
        loader.abolish();
        unsetView();
    }

    @Override
    public void remove() {
        dispose();
    }

    @Override
    public void prerender() {
        View.ImageData current = imageData;
        if (current == null) {
            return;
        }
        View.ImageData comparisonData = comparisonImageData(current);
        ImageBuffer differenceBuffer = displaySettings.getDifferenceMode() == DifferenceMode.None
                ? null : comparisonData.imageBuffer();
        glImage.streamImages(current.imageBuffer(), differenceBuffer);
    }

    @Override
    public void renderMiniview(MapView mv, Viewport vp) {
        render(mv, vp);
    }

    @Override
    public void renderScale(MapView mv, Viewport vp) {
        render(mv, vp);
    }

    private final float[] crval0 = new float[2];
    private final float[] crval1 = new float[2];

    @Override
    public void render(MapView mv, Viewport vp) {
        View.ImageData current = imageData;
        if (current == null) {
            return;
        }
        if (!isVisible[vp.idx])
            return;

        MetaData meta0 = current.metaData();
        glImage.applyFilters(current.imageBuffer(), meta0, getFilter() == ImageFilter.Type.RHEF);

        Position metaViewpoint0 = meta0.getViewpoint();
        View.ImageData imageDataDiff = comparisonImageData(current);
        MetaData meta1 = imageDataDiff.metaData();
        Position metaViewpoint1 = meta1.getViewpoint();
        WcsHeader wcs0 = meta0.getWcsHeader();
        WcsHeader wcs1 = meta1.getWcsHeader();

        Quat q = mv.viewRotation();
        Quat cameraDiff0 = Quat.rotateWithConjugate(q, metaViewpoint0.toQuat());
        Quat cameraDiff1 = Quat.rotateWithConjugate(q, metaViewpoint1.toQuat());

        Mat2 planeToImage0 = wcs0.planeToImage;
        Mat2 planeToImage1 = wcs1.planeToImage;
        double deltaCROTA = displaySettings.getDeltaCROTA();
        if (deltaCROTA != 0) {
            // The user rotation follows the metadata image-to-plane transform,
            // so it precedes that transform's inverse in plane-to-image order.
            Mat2 inverseAdjustment = Mat2.rotation(Math.toRadians(-deltaCROTA));
            planeToImage0 = Mat2.multiply(planeToImage0, inverseAdjustment);
            planeToImage1 = Mat2.multiply(planeToImage1, inverseAdjustment);
        }

        double deltaCRVAL1 = displaySettings.getDeltaCRVAL1();
        if (deltaCRVAL1 == 0) {
            crval0[0] = (float) wcs0.crval.x;
            crval1[0] = (float) wcs1.crval.x;
        } else {
            crval0[0] = (float) (wcs0.crval.x + deltaCRVAL1 * meta0.getUnitPerArcsec());
            crval1[0] = (float) (wcs1.crval.x + deltaCRVAL1 * meta1.getUnitPerArcsec());
        }

        double deltaCRVAL2 = displaySettings.getDeltaCRVAL2();
        if (deltaCRVAL2 == 0) {
            crval0[1] = (float) wcs0.crval.y;
            crval1[1] = (float) wcs1.crval.y;
        } else {
            crval0[1] = (float) (wcs0.crval.y + deltaCRVAL2 * meta0.getUnitPerArcsec());
            crval1[1] = (float) (wcs1.crval.y + deltaCRVAL2 * meta1.getUnitPerArcsec());
        }

        float deltaT0 = 0, deltaT1 = 0;
        Position renderViewpoint = mv.viewpoint();
        if (ImageLayers.getDiffRotationMode()) {
            deltaT0 = (float) ((renderViewpoint.time.milli - metaViewpoint0.time.milli) * 1e-9);
            deltaT1 = (float) ((renderViewpoint.time.milli - metaViewpoint1.time.milli) * 1e-9);
        }

        Quat sourceView0 = wcs0.projection.isSurfaceMap() ? q : metaViewpoint0.toQuat();
        Quat sourceView1 = wcs1.projection.isSurfaceMap() ? q : metaViewpoint1.toQuat();

        GLSLImageShader.bindImages(
                current.region(), planeToImage0, crval0, wcs0,
                (float) metaViewpoint0.distance, deltaT0, cameraDiff0, sourceView0,
                imageDataDiff.region(), planeToImage1, crval1, wcs1,
                (float) metaViewpoint1.distance, deltaT1, cameraDiff1, sourceView1);
        GLSLImageShader.render(mv.mode(), wcs0.pv2, wcs1.pv2);
    }

    @Nonnull
    private View.ImageData comparisonImageData(@Nonnull View.ImageData current) {
        View.ImageData comparison = displaySettings.getDifferenceMode() == DifferenceMode.Base ? baseImageData : prevImageData;
        return comparison == null ? current : comparison;
    }

    @Override
    public String getName() {
        return imageData == null ? "Loading..." : imageData.metaData().getDisplayName();
    }

    @Nullable
    @Override
    public String getTimeString() {
        return imageData == null ? null : imageData.metaData().getViewpoint().time.toString();
    }

    @Override
    public boolean isDeletable() {
        return true;
    }

    @Override
    public void dispose() {
        glImage.dispose();
    }

    @Nullable
    private View.ImageData imageData;
    @Nullable
    private View.ImageData prevImageData;
    @Nullable
    private View.ImageData baseImageData;

    @Nullable
    private static View.ImageData replaceImageData(@Nullable View.ImageData previous, @Nullable View.ImageData next) {
        if (next != null)
            next.image().retain();
        if (previous != null)
            previous.image().release();
        return next;
    }

    private void clearImageData() {
        imageData = replaceImageData(imageData, null);
        prevImageData = replaceImageData(prevImageData, null);
        baseImageData = replaceImageData(baseImageData, null);
    }

    private void setImageData(@Nonnull View.ImageData newImageData) {
        long newMilli = newImageData.metaData().getViewpoint().time.milli;
        boolean base = baseImageData == null || newMilli == view.getFirstTime().milli;
        if (base) {
            baseImageData = replaceImageData(baseImageData, newImageData);
        }

        if (imageData == null || base) { // first or loop playback
            prevImageData = replaceImageData(prevImageData, newImageData);
        } else if (newMilli != imageData.metaData().getViewpoint().time.milli) { // new frame
            prevImageData = replaceImageData(prevImageData, imageData);
        }

        imageData = replaceImageData(imageData, newImageData);
    }

    @Nullable
    public View.ImageData getImageData() {
        return imageData;
    }

    @Nonnull
    public MetaData getMetaData() { //!
        return imageData == null ? view.getMetaData(view.getFirstTime()) : imageData.metaData();
    }

    @Override
    public void handleData(@Nonnull View.ImageData newImageData) {
        String oldName = getName();

        setImageData(newImageData);

        if (!Objects.equals(oldName, getName()))
            Layers.fireNameUpdated(this);
        Layers.fireTimeUpdated(this);

        ImageLayers.displaySynced(newImageData.viewpoint());
    }

    @Override
    public boolean isDownloading() {
        return loader.isLoading() || view.isDownloading();
    }

    @Override
    public boolean isLocal() {
        return view.getAPIRequest() == null;
    }

    @Nonnull
    public ImageDisplaySettings getDisplaySettings() {
        return displaySettings;
    }

    @Nonnull
    public View getView() {
        return view;
    }

    public boolean isLoadingForTimespan() {
        return loader.isLoading();
    }

    public long getStartTime() {
        APIRequest req = view.getAPIRequest(); // for locked timelines
        return req == null ? view.getFirstTime().milli : req.startTime();
    }

    public long getEndTime() {
        APIRequest req = view.getAPIRequest(); // for locked timelines
        return req == null ? view.getLastTime().milli : req.endTime();
    }

    public boolean isViewLoadFinished() {
        return !loader.isLoading() && view.getFrameCompletion(view.getMaximumFrameNumber()) != null;
    }

    public void cancelDownloadTask() {
        loader.cancelDownload();
    }

    public void startDownload(DownloadLayer.Progress progress) {
        APIRequest req = view.getAPIRequest();
        String baseName = view.getBaseName();
        if (req != null && baseName != null) // should not happen
            loader.startDownload(req, this, baseName, progress);
    }

}
