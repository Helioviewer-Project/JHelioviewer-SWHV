package org.helioviewer.jhv.source;

import java.awt.EventQueue;
import java.io.IOException;
import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.ClipSet;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBufferCache;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.metadata.BasicMetaData;
import org.helioviewer.jhv.metadata.FitsMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.movie.ExportMovie;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.thread.LatestWorker;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;
import org.helioviewer.jhv.view.BaseView;

public final class J2KView extends BaseView implements Source.Listener {

    private static final AtomicInteger globalSerial = new AtomicInteger();

    private static final Cleaner reaper = Cleaner.create();
    private final Cleaner.Cleanable abolishable;

    private final APIRequest request;
    private final int serial;

    private final J2KSource source;
    private final int maxFrame;
    private int targetFrame;
    private Position currentViewpoint;
    private J2KParams.Decode requestedDecode;

    private final String[] xmlMetaData;
    private final TimeMap<Integer> frameMap = new TimeMap<>();

    public J2KView(LatestWorker<DecodedImage> _executor, APIRequest _request, DataUri _dataUri, ImageProcessingSettings _processingSettings) throws Exception {
        super(_executor, _dataUri, _processingSettings);
        serial = globalSerial.incrementAndGet();
        request = _request;

        J2KSource acquired = null;
        try {
            source = acquired = switch (dataUri.format()) {
                case JPIP -> J2KSource.open(dataUri.uri());
                case JP2, JPX -> new J2KSource(dataUri.file().toPath());
                default -> throw new Exception("Unknown image type");
            };

            builtinLUT = source.lut();

            maxFrame = source.frames() - 1;
            metaData = new MetaData[maxFrame + 1];
            xmlMetaData = new String[maxFrame + 1];
            for (int i = 0; i <= maxFrame; i++) {
                try {
                    xmlMetaData[i] = source.xml(i);
                    if (xmlMetaData[i] == null)
                        throw new Exception("Missing XML metadata");
                    metaData[i] = new FitsMetaData(new XMLMetaDataContainer(xmlMetaData[i]), dataUri.sourceUri());
                } catch (Exception e) {
                    xmlMetaData[i] = EMPTY_METAXML;
                    // A JPIP frame's own size may not be known yet.
                    ResolutionSet set = source.geometry(i);
                    ResolutionSet.Level level = (set == null ? source.levels(0) : set).getLevel(0);
                    metaData[i] = new BasicMetaData(level.width(), level.height(), dataUri.baseName(), dataUri.sourceUri());
                    Log.warn("Helioviewer metadata missing for layer " + i, e);
                }
                JHVTime time = metaData[i].getViewpoint().time;
                if (i > 0) {
                    JHVTime previous = metaData[i - 1].getViewpoint().time;
                    if (time.milli <= previous.milli)
                        throw new Exception((time.milli == previous.milli ? "Duplicate frame timestamp" : "Out-of-order frame timestamp")
                                + "\nFrame " + (i - 1) + ": " + previous + "\nFrame " + i + ": " + time);
                }
                frameMap.put(time, i);
            }
            frameMap.buildIndex();

            String[] cacheKey = new String[maxFrame + 1];
            if (request != null) {
                for (int i = 0; i <= maxFrame; i++) {
                    if (metaData[i] instanceof FitsMetaData)
                        cacheKey[i] = request.sourceId() + "+" + metaData[i].getViewpoint().time.milli;
                }
            }
            source.setListener(this);
            source.start(cacheKey);

            abolishable = reaper.register(this, new J2KAbolisher(serial, new WeakReference<>(this), source));
        } catch (Exception e) {
            if (acquired != null)
                acquired.close();
            throw new Exception(e.getMessage() + ": " + dataUri, e);
        }
    }

    private record DecodeKey(int serial, J2KParams.Decode params, ImageFilter.Type filter) implements ImageBufferCache.Key {
        @Override
        public Object owner() {
            return serial;
        }
    }

    private static void clearCache(int aSerial) {
        ImageBufferCache.invalidateOwners(Set.of(aSerial));
    }

    private record J2KAbolisher(int aSerial, WeakReference<J2KView> aView, J2KSource aSource) implements Runnable {
        @Override
        public void run() {
            // Explicit owners have already cleared the cache, possibly in one collection-wide pass.
            if (aView.get() == null)
                clearCache(aSerial);
            // Stopping a reader may take too long in stressed conditions.
            AppThread.create(aSource::close, "JHV-J2KAbolisher").start();
        }
    }

    @Nullable
    @Override
    public APIRequest getAPIRequest() {
        return request;
    }

    @Override
    public void abolish() {
        clearCache();
        closeSources();
    }

    @Override
    public void collectCacheOwners(Set<Object> owners) {
        owners.add(serial);
    }

    @Override
    public void closeSources() {
        try {
            abolishable.clean();
        } finally {
            // Keep the Cleaner action on its explicit-close path until clean() returns.
            Reference.reachabilityFence(this);
        }
    }

    @Override
    public boolean isMultiFrame() {
        return maxFrame > 0;
    }

    @Override
    public int getCurrentFrameNumber() {
        return targetFrame;
    }

    @Override
    public int getMaximumFrameNumber() {
        return maxFrame;
    }

    @Override
    public JHVTime getFirstTime() {
        return frameMap.firstKey();
    }

    @Override
    public JHVTime getLastTime() {
        return frameMap.lastKey();
    }

    @Override
    public boolean setNearestFrame(JHVTime time) {
        int frame = frameMap.nearestIndex(time);
        if (frame != targetFrame) {
            if (frame > source.getPartialUntil())
                return false;
            targetFrame = frame;
        }
        return true;
    }

    @Override
    public JHVTime getFrameTime(int frame) {
        return frameMap.key(frame);
    }

    @Override
    public JHVTime getNearestTime(JHVTime time) {
        return frameMap.nearestKey(time);
    }

    @Override
    public JHVTime getLowerTime(JHVTime time) {
        return frameMap.lowerKey(time);
    }

    @Override
    public JHVTime getHigherTime(JHVTime time) {
        return frameMap.higherKey(time);
    }

    @Override
    public MetaData getMetaData(JHVTime time) {
        return metaData[frameMap.nearestIndex(time)];
    }

    @Override
    public boolean isDownloading() {
        return source.isDownloading();
    }

    private J2KParams.Decode getDecodeParams(int frame, double pixFactor) {
        ResolutionSet.Level res;
        if (ExportMovie.isRecording()) { // all bets are off
            res = source.levels(frame).getLevel(0);
        } else {
            MetaData m = metaData[frame];
            int reqWidth = (int) (m.getPhysicalRegion().width * pixFactor + .5);
            int reqHeight = (int) (m.getPhysicalRegion().height * pixFactor + .5);
            res = source.levels(frame).getNextLevel(reqWidth, reqHeight);
        }

        return new J2KParams.Decode(frame, res.level());
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, @Nullable ClipSet.Range clipRange) {
        currentViewpoint = viewpoint;
        J2KParams.Decode wanted = getDecodeParams(targetFrame, pixFactor);
        requestedDecode = wanted;
        J2KParams.Decode decodeParams = available(wanted); // before requesting from the reader
        source.request(wanted.frame(), wanted.level(), !Player.isPlaying());
        show(decodeParams);
    }

    // The request itself, or the whole finest complete level while a JPIP frame lacks the wanted one.
    private J2KParams.Decode available(J2KParams.Decode wanted) {
        ResolutionSet.Level res = source.levels(wanted.frame()).getCompleteLevel(wanted.level());
        return res.level() == wanted.level() ? wanted : new J2KParams.Decode(wanted.frame(), res.level());
    }

    @Override
    public void frameUpdated(Source _source, int frame) {
        refreshDecodeFromReader(frame);
    }

    void refreshDecodeFromReader(int frame) {
        EventQueue.invokeLater(() -> {
            if (dataHandler != null && requestedDecode != null && frame == targetFrame && frame == requestedDecode.frame()) {
                show(available(requestedDecode));
            }
        });
    }

    private void show(J2KParams.Decode decodeParams) {
        DecodeKey key = new DecodeKey(serial, decodeParams, processingSettings.getFilter());
        DecodedImage image = ImageBufferCache.get(key);
        if (image != null) {
            // Mark running decodes stale before publishing this cached result.
            executor.invalidate();
            sendDataToHandler(decodeParams.frame(), currentViewpoint, image, () -> key.filter == processingSettings.getFilter());
            return;
        }

        MetaData m = metaData[decodeParams.frame()];
        executor.submit(
                key,
                () -> decodeImage(decodeParams, key.filter, m),
                decodeCallback(key, decodeParams.frame(), currentViewpoint,
                        () -> key.filter == processingSettings.getFilter()));
    }

    // Runs on the decode worker; the view owns solar geometry and image filtering.
    private DecodedImage decodeImage(J2KParams.Decode params, ImageFilter.Type filterType, MetaData metadata) throws IOException {
        ResolutionSet.Level full = source.levels(params.frame()).getLevel(0);
        Region imageRegion = metadata.imageToRegion(full.width(), full.height());
        ImageFilter filter = ImageFilter.of(filterType, imageRegion, metadata);
        return new DecodedImage(source.decode(params.frame(), params.level(), filter, null, null), imageRegion);
    }

    @Nullable
    @Override
    public Boolean getFrameCompletion(int frame) {
        return source.completion(frame);
    }

    @Override
    public boolean isComplete() {
        return source.isComplete();
    }

    @Nonnull
    @Override
    public String getXMLMetaData(JHVTime time) {
        return xmlMetaData[frameMap.nearestIndex(time)];
    }

}
