package org.helioviewer.jhv.layers;

import java.net.URI;
import java.util.Arrays;
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
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.opengl.GLSLImage;
import org.helioviewer.jhv.opengl.GLSLImageShader;
import org.helioviewer.jhv.source.Source;
import org.helioviewer.jhv.wcs.ImageGeometry;

import org.json.JSONObject;

public class ImageLayer extends AbstractLayer implements FrameDecoder.Target, Frames.Listener {

    private final boolean[] isVisible = {false, false, false, false, false, false}; // match max of Display.viewports.length
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
        if (req.equals(request))
            loader.cancelLoad();
        else
            loader.load(req);
        Layers.fireLayerUpdated(this); // give feedback asap
        settleLoad();
    }

    // Re-requests a remote layer over another span; a local layer has nothing to reload.
    void reload(long start, long end, int cadence) {
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
        Arrays.fill(isVisible, enabled);
        ImageLayers.arrangeMultiView();
    }

    public void setVisible(int j) {
        Arrays.fill(isVisible, false);
        if (j >= 0 && j < isVisible.length)
            isVisible[j] = true;
    }

    public boolean isVisible(int idx) {
        return isVisible[idx];
    }

    public int isVisibleIdx() {
        for (int i = 0; i < isVisible.length; i++) {
            if (isVisible[i])
                return i;
        }
        return -1;
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
    @Override
    public void detach() {
        removed = true;
        loader.abolish();
        decoder.dispose();
        frames.setListener(null);
        frames.close();
        clearImageData();
        settleLoad();
    }

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
    public void renderScale(MapView mv, Viewport vp) {
        render(mv, vp);
    }

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
        ImageGeometry geometry0 = current.image().geometry();
        ImageGeometry geometry1 = imageDataDiff.image().geometry();
        double deltaCROTA = displaySettings.getDeltaCROTA();
        Mat2 adjustment = deltaCROTA == 0 ? Mat2.IDENTITY : Mat2.rotation(Math.toRadians(-deltaCROTA));

        double deltaCRVAL1 = displaySettings.getDeltaCRVAL1();
        double deltaCRVAL2 = displaySettings.getDeltaCRVAL2();

        GLSLImageShader.render(mv.mode(), mv.viewRotation(), mv.viewpoint(),
                geometry0, metaViewpoint0,
                geometry1, metaViewpoint1,
                adjustment, deltaCRVAL1, deltaCRVAL2, ImageLayers.getDiffRotationMode());
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
        boolean base = baseImageData == null || newMilli == frames.firstTime().milli;
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
        return imageData == null ? frames.metaData(frames.firstTime()) : imageData.metaData();
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
        boolean moved = frames.select(frames.requested());
        Layers.fireCompletionUpdated(this);
        if (isVisibleIdx() != -1) {
            Frames.Frame current = frames.valueAt(frames.current());
            if (moved)
                decoder.redecode(frames, true);
            else if (current.source() == source && current.index() == frame)
                decoder.redecode(frames, false);
        }
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

    public long getStartTime() {
        return request == null ? frames.firstTime().milli : request.startTime(); // for locked timelines
    }

    public long getEndTime() {
        return request == null ? frames.lastTime().milli : request.endTime(); // for locked timelines
    }

    private boolean isLoadFinished() {
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
