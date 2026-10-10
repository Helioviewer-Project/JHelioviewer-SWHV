package org.helioviewer.jhv.layers;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
import org.helioviewer.jhv.opengl.GLSLImage;
import org.helioviewer.jhv.opengl.GLSLImageShader;
import org.helioviewer.jhv.source.Source;
import org.helioviewer.jhv.wcs.WcsHeader;

import org.json.JSONObject;

public class ImageLayer extends AbstractLayer implements FrameDecoder.Target, Frames.Listener {

    private final ImageDisplaySettings displaySettings = new ImageDisplaySettings();
    private final ImageProcessingSettings processingSettings = new ImageProcessingSettings(this::refreshImage);
    private final GLSLImage glImage = new GLSLImage(displaySettings);
    private final FrameDecoder decoder = new FrameDecoder(processingSettings, this);
    private final ImageLayerLoader loader = new ImageLayerLoader(this::loaded, this::loadFailed);

    private Frames frames;
    @Nullable
    private APIRequest request;
    @Nullable
    private String baseName;
    private boolean loaded;
    private boolean removed;
    @Nullable
    private CompletableFuture<Boolean> loadFinished;

    public static ImageLayer create() {
        ImageLayer imageLayer = new ImageLayer(Frames.placeholder());
        Layers.add(imageLayer);
        return imageLayer;
    }

    // Only for state restore, which batches layer registration.
    public static ImageLayer createDetached(JSONObject jo, APIRequest _request) {
        ImageLayer imageLayer = new ImageLayer(Frames.placeholder());
        imageLayer.applyImageParams(jo.optJSONObject("imageParams"));
        imageLayer.load(_request);
        return imageLayer;
    }

    // The placeholder layer, and the start of every other.
    ImageLayer(Frames _frames) {
        frames = _frames;
        frames.setListener(this);
        decoder.reset(frames.serial());
    }

    @Override
    public void serialize(JSONObject jo) {
        if (request != null) {
            jo.put("APIRequest", request.toJson());
            JSONObject imageParams = displaySettings.toJson();
            processingSettings.serialize(imageParams);
            jo.put("imageParams", imageParams);
        }
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

    void decode(Position viewpoint, double pixFactor, boolean priority) {
        decoder.decode(frames, viewpoint, pixFactor, priority);
    }

    public ImageProcessingSettings getProcessingSettings() {
        return processingSettings;
    }

    private void refreshImage() {
        if (removed)
            return;
        frames.clearCache();
        clearImageData();
        DisplayController.render();
    }

    public void load(APIRequest req) {
        if (removed)
            return;
        if (req.equals(request)) {
            loader.cancelLoad();
            Layers.fireLayerUpdated(this);
            settleLoad();
            return;
        }

        loader.load(req);
        Layers.fireLayerUpdated(this); // give feedback asap
    }

    // Re-requests a remote layer over another span; a local layer has nothing to reload.
    public void reload(long start, long end, int cadence) {
        if (request != null)
            load(request.withSpan(start, end, cadence));
    }

    public void load(List<URI> uris) {
        if (removed)
            return;

        loader.load(uris);
        Layers.fireLayerUpdated(this); // give feedback asap
    }

    private void loadFailed() {
        if (loaded)
            Layers.fireLayerUpdated(this);
        else
            Layers.remove(this);
        settleLoad();
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

    private void loaded(ImageLayerLoader.Result result) {
        boolean firstLoad = !loaded;
        loaded = true;
        request = result.request();
        baseName = result.baseName();
        setFrames(result.frames());
        displaySettings.setLUT(frames.defaultLUT(), displaySettings.getInvertLUT());
        Layers.imageLayerLoaded(this, firstLoad);
        settleLoad();
    }

    // One replacement: outstanding decodes of the old timeline are dropped before it is closed.
    void setFrames(Frames next) {
        frames.setListener(null);
        decoder.reset(next.serial());
        clearImageData();
        Frames old = frames;
        frames = next;
        frames.setListener(this);
        old.close();
    }

    // Release the CPU-side owners now; GL disposal still needs the render context.
    void detach() {
        removed = true;
        loader.abolish();
        decoder.dispose();
        frames.setListener(null);
        frames.close();
        clearImageData();
        settleLoad();
    }

    @Override
    public void remove() {
        dispose();
    }

    @Override
    public void prerender() {
        ImageData current = imageData;
        if (current == null) {
            return;
        }
        ImageData comparisonData = comparisonImageData(current);
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
        ImageData current = imageData;
        if (current == null) {
            return;
        }
        if (!isVisible[vp.idx])
            return;

        MetaData meta0 = current.metaData();
        glImage.applyFilters(current.imageBuffer(), meta0, getFilter() == ImageFilter.Type.RHEF);

        Position metaViewpoint0 = meta0.getViewpoint();
        ImageData imageDataDiff = comparisonImageData(current);
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
    private ImageData comparisonImageData(@Nonnull ImageData current) {
        ImageData comparison = displaySettings.getDifferenceMode() == DifferenceMode.Base ? baseImageData : prevImageData;
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
    private ImageData imageData;
    @Nullable
    private ImageData prevImageData;
    @Nullable
    private ImageData baseImageData;

    @Nullable
    private static ImageData replaceImageData(@Nullable ImageData previous, @Nullable ImageData next) {
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

    private void setImageData(@Nonnull ImageData newImageData) {
        long newMilli = newImageData.frame().time().milli;
        boolean base = baseImageData == null || newMilli == frames.first().milli;
        if (base) {
            baseImageData = replaceImageData(baseImageData, newImageData);
        }

        if (imageData == null || base) { // first or loop playback
            prevImageData = replaceImageData(prevImageData, newImageData);
        } else if (newMilli != imageData.frame().time().milli) { // new frame
            prevImageData = replaceImageData(prevImageData, imageData);
        }

        imageData = replaceImageData(imageData, newImageData);
    }

    @Nullable
    public ImageData getImageData() {
        return imageData;
    }

    @Nonnull
    public MetaData getMetaData() { //!
        return imageData == null ? frames.metaData(frames.first()) : imageData.metaData();
    }

    @Override
    public void handleData(@Nonnull ImageData newImageData) {
        String oldName = getName();

        setImageData(newImageData);

        if (!Objects.equals(oldName, getName()))
            Layers.fireNameUpdated(this);
        Layers.fireTimeUpdated(this);

        ImageLayers.displaySynced(newImageData.viewpoint());
    }

    // A frame arrived: catch up with the requested time, or refine the frame already shown.
    @Override
    public void frameReady(Frames _frames, Source source, int frame) {
        if (_frames != frames)
            return;
        Layers.fireCompletionUpdated(this);
        boolean moved = frames.select(frames.requested());
        Frames.Frame current = frames.get(frames.current());
        if (moved)
            decoder.redecode(frames, true);
        else if (current.source() == source && current.index() == frame)
            decoder.redecode(frames, false);
        settleLoad();
    }

    @Override
    public boolean isDownloading() {
        return loader.isLoading() || frames.isDownloading();
    }

    @Override
    public boolean isLocal() {
        return request == null;
    }

    @Nonnull
    public ImageDisplaySettings getDisplaySettings() {
        return displaySettings;
    }

    @Nonnull
    public Frames frames() {
        return frames;
    }

    @Nullable
    public APIRequest getAPIRequest() {
        return request;
    }

    public boolean isComplete() {
        return frames.isComplete();
    }

    // Snapshot: null when unavailable, false when partial, true when complete.
    @Nullable
    public Boolean frameCompletion(int frame) {
        return frames.completion(frame);
    }

    public boolean isLoadingForTimespan() {
        return loader.isLoading();
    }

    public long getStartTime() {
        return request == null ? frames.first().milli : request.startTime(); // for locked timelines
    }

    public long getEndTime() {
        return request == null ? frames.last().milli : request.endTime(); // for locked timelines
    }

    public boolean isLoadFinished() {
        return !loader.isLoading() && frames.completion(frames.size() - 1) != null;
    }

    // Completes with whether the layer is still registered once its load has finished. EDT only.
    public CompletableFuture<Boolean> whenLoadFinished() {
        CompletableFuture<Boolean> future = loadFinished;
        if (future == null) {
            future = new CompletableFuture<>();
            loadFinished = future;
            settleLoad(); // may complete it at once
        }
        return future;
    }

    private void settleLoad() {
        if (loadFinished == null || (!removed && !isLoadFinished()))
            return;
        CompletableFuture<Boolean> future = loadFinished;
        loadFinished = null;
        future.complete(!removed);
    }

    public void cancelDownloadTask() {
        loader.cancelDownload();
    }

    public void startDownload(DownloadLayer.Progress progress) {
        if (request != null && baseName != null) // should not happen
            loader.startDownload(request, baseName, progress, path -> load(List.of(path.toUri())));
    }

}
