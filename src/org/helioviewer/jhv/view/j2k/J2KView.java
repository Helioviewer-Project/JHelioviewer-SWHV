package org.helioviewer.jhv.view.j2k;

import java.awt.EventQueue;
import java.io.IOException;
import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageBufferCache;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.image.lut.LUT;
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
import org.helioviewer.jhv.view.ClipSet;

public final class J2KView extends BaseView {

    private static final AtomicInteger globalSerial = new AtomicInteger();

    private static final Cleaner reaper = Cleaner.create();
    private final Cleaner.Cleanable abolishable;
    @SuppressWarnings("FieldCanBeLocal")
    // Cleaner tracks reachability of this token; the cleanup action must not capture this J2KView.
    private final Object cleanerToken = new Object();

    private final APIRequest request;
    private final int serial;

    private final J2KSource source;
    private final int maxFrame;
    private int targetFrame;
    private Position currentViewpoint;

    private final String[] xmlMetaData;
    private final TimeMap<Integer> frameMap = new TimeMap<>();

    private final J2KReader reader;

    public J2KView(LatestWorker<DecodedImage> _executor, APIRequest _request, DataUri _dataUri, ImageProcessingSettings _processingSettings) throws Exception {
        super(_executor, _dataUri, _processingSettings);
        serial = globalSerial.incrementAndGet();
        request = _request;

        J2KSource acquiredSource = null;
        J2KReader acquiredReader = null;
        try {
            switch (dataUri.format()) {
                case JPIP -> {
                    source = acquiredSource = new J2KSource(null);
                    reader = acquiredReader = new J2KReader(dataUri.uri(), source);
                }
                case JP2, JPX -> {
                    reader = null;
                    source = acquiredSource = new J2KSource(dataUri.file().toPath());
                }
                default -> throw new Exception("Unknown image type");
            }

            LUT lut = source.lut();
            if (lut != null)
                builtinLUT = lut;

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
                    ResolutionSet.Level level = (set == null ? source.resolutionSet(0) : set).getLevel(0);
                    metaData[i] = new BasicMetaData(level.width(), level.height(), dataUri.baseName(), dataUri.sourceUri());
                    Log.warn("Helioviewer metadata missing for layer " + i, e);
                }
                if (frameMap.put(metaData[i].getViewpoint().time, i) != null) // log duplicated
                    Log.warn("Duplicate time stamp: " + metaData[i].getViewpoint().time);
            }
            frameMap.buildIndex();
            if (frameMap.maxIndex() != maxFrame)
                throw new Exception("Duplicated time stamps");

            if (reader != null) {
                String[] cacheKey = new String[maxFrame + 1];
                if (request != null) {
                    for (int i = 0; i <= maxFrame; i++) {
                        if (frameMap.key(i).milli != metaData[i].getViewpoint().time.milli)
                            Log.warn("Badly ordered metadata: " + dataUri + "[" + i + "]: expected " + frameMap.key(i) + ", got " + metaData[i].getViewpoint().time);
                        if (metaData[i] instanceof FitsMetaData)
                            cacheKey[i] = request.sourceId() + "+" + metaData[i].getViewpoint().time.milli;
                    }
                }
                reader.setCacheKey(cacheKey);
            }

            abolishable = reaper.register(cleanerToken, new J2KAbolisher(serial, reader, source));
        } catch (Exception e) {
            if (acquiredReader != null)
                acquiredReader.stop();
            if (acquiredSource != null)
                acquiredSource.close();
            throw new Exception(e.getMessage() + ": " + dataUri, e);
        }
    }

    private static void clearCache(int aSerial) {
        ImageBufferCache.invalidateIf(key -> key instanceof J2KDecodeKey dk && dk.serial() == aSerial);
    }

    private record J2KAbolisher(int aSerial, J2KReader aReader, J2KSource aSource) implements Runnable {
        @Override
        public void run() {
            // reader abolish may take too long in stressed conditions
            AppThread.create(() -> {
                try {
                    if (aReader != null) {
                        aReader.stop();
                    }
                    aSource.close();
                } finally {
                    clearCache(aSerial);
                }
            }, "JHV-J2KAbolisher").start();
        }
    }

    @Nullable
    @Override
    public APIRequest getAPIRequest() {
        return request;
    }

    @Override
    public void abolish() {
        abolishable.clean();
    }

    @Override
    public void clearCache() {
        clearCache(serial);
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
        int frame = frameMap.nearestValue(time);
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
        return metaData[frameMap.nearestValue(time)];
    }

    private volatile boolean isDownloading;

    void setDownloading(boolean val) {
        isDownloading = val;
    }

    @Override
    public boolean isDownloading() {
        return isDownloading;
    }

    private J2KParams.Decode getDecodeParams(int frame, double pixFactor) {
        ResolutionSet.Level res;
        if (ExportMovie.isRecording()) { // all bets are off
            res = source.resolutionSet(frame).getLevel(0);
        } else {
            MetaData m = metaData[frame];
            int reqHeight = (int) (m.getPhysicalRegion().height * pixFactor + .5);
            res = source.resolutionSet(frame).getNextLevel(reqHeight, reqHeight);
        }

        return new J2KParams.Decode(frame, res.subImage(), res.level());
    }

    private static final int NO_LEVEL = 10000;
    private int currentLevel = NO_LEVEL;

    private void signalReader(J2KParams.Decode decodeParams) {
        int level = decodeParams.level;
        boolean priority = !Player.isPlaying();

        if (priority || level < currentLevel) {
            reader.signal(new J2KParams.Read(this, decodeParams, priority));
            currentLevel = level;
        }
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, @Nullable ClipSet.Range clipRange) {
        currentViewpoint = viewpoint;
        J2KParams.Decode wanted = getDecodeParams(targetFrame, pixFactor);
        J2KParams.Decode decodeParams = available(wanted); // before signalling to reader
        // The first signal starts the movie download.
        if (reader != null && (decodeParams != wanted || currentLevel == NO_LEVEL)) {
            signalReader(wanted);
        }
        show(decodeParams, viewpoint);
    }

    // The request itself, or the whole finest complete level while a JPIP frame lacks the wanted one.
    private J2KParams.Decode available(J2KParams.Decode wanted) {
        ResolutionSet.Level res = source.resolutionSet(wanted.frame).getCompleteLevel(wanted.level);
        return res.level() == wanted.level ? wanted : new J2KParams.Decode(wanted.frame, res.subImage(), res.level());
    }

    void refreshDecodeFromReader(J2KParams.Decode wanted) {
        EventQueue.invokeLater(() -> {
            if (dataHandler != null && wanted.frame == targetFrame) {
                show(available(wanted), currentViewpoint);
            }
        });
    }

    private void show(J2KParams.Decode decodeParams, Position viewpoint) {
        J2KDecodeKey key = new J2KDecodeKey(serial, decodeParams, processingSettings.getFilter());
        DecodedImage image = ImageBufferCache.get(key);
        if (image != null) {
            // Mark running decodes stale before publishing this cached result.
            executor.invalidate();
            sendDataToHandler(decodeParams.frame, viewpoint, image, () -> key.filter() == processingSettings.getFilter());
            return;
        }

        MetaData m = metaData[decodeParams.frame];
        ResolutionSet.Level resolution = getResolutionLevel(decodeParams.frame, decodeParams.level);
        executor.submit(
                () -> decodeImage(decodeParams, key.filter(), m, resolution),
                new J2KCallback(key, viewpoint));
    }

    // Runs on the decode worker; the view owns solar geometry and image filtering.
    private DecodedImage decodeImage(J2KParams.Decode params, ImageFilter.Type filterType, MetaData metadata, ResolutionSet.Level resolution) throws IOException {
        try (J2KNative.Decode job = source.beginDecode(params.frame, params.level)) {
            J2KParams.SubImage roi = params.subImage;
            Region imageRegion = metadata.roiToRegion(roi.x(), roi.y(), roi.w(), roi.h(), resolution.factorX(), resolution.factorY());
            ImageFilter filter = ImageFilter.of(filterType, imageRegion, metadata);
            boolean gray = source.resolutionSet(params.frame).numComps == 1;
            ImageBuffer.WriteBuffer outBuffer = ImageBuffer.createWriteBuffer(roi.w(), roi.h(), gray ? ImageBuffer.Format.Gray8 : ImageBuffer.Format.RGBA32, filter);
            J2KSource.decode(job, roi.x(), roi.y(), roi.w(), roi.h(), outBuffer.byteBuffer());
            return new DecodedImage(outBuffer.finish(), imageRegion);
        }
    }

    private class J2KCallback implements LatestWorker.Callback<DecodedImage> {

        private final J2KDecodeKey key;
        private final Position viewpoint;

        J2KCallback(J2KDecodeKey _key, Position _viewpoint) {
            key = _key;
            viewpoint = _viewpoint;
        }

        @Override
        public void onSuccess(DecodedImage result, boolean fresh) {
            if (dataHandler == null || key.filter() != processingSettings.getFilter()) return; // detached or filter changed in-flight
            ImageBufferCache.put(key, result);

            // This decode was superseded after it started; do not publish it to the layer.
            if (!fresh) return;
            sendDataToHandler(key.params().frame, viewpoint, result, () -> key.filter() == processingSettings.getFilter());
        }

        @Override
        public void onFailure(@Nonnull Throwable t, boolean fresh) {
            if (dataHandler != null)
                Log.errorStack(t);
        }

    }

    @Nullable
    @Override
    public AtomicBoolean getFrameCompletion(int frame) {
        return source.getFrameStatus(frame, currentLevel);
    }

    @Override
    public boolean isComplete() {
        return source.isComplete(currentLevel);
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return xmlMetaData[targetFrame];
    }

    ResolutionSet.Level getResolutionLevel(int frame, int level) {
        return source.resolutionSet(frame).getLevel(level);
    }

}
