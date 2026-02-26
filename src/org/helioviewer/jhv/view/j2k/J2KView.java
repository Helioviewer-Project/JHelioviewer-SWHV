package org.helioviewer.jhv.view.j2k;

import java.awt.EventQueue;
import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import kdu_jni.KduException;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageFilter;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.DataUri.Format.Image;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;
import org.helioviewer.jhv.view.BaseView;
import org.helioviewer.jhv.view.DecodeCallback;
import org.helioviewer.jhv.view.DecodeExecutor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class J2KView extends BaseView {

    private record DecodeKey(J2KParams.Decode params, ImageFilter.Type filter) {
    }

    private static final Cache<DecodeKey, ImageBuffer> decodeCache = Caffeine.newBuilder().softValues().build();
    private static final Cleaner reaper = Cleaner.create();
    private static final AtomicInteger globalSerial = new AtomicInteger();

    private final Cleaner.Cleanable abolishable;
    // cleaner tracks reachability of this token
    private final Object cleanerToken = new Object();

    private final APIRequest request;
    protected final int serial;

    private final J2KSource source;
    private final int maxFrame;
    private int targetFrame;

    private final String[] xmlMetaData;
    private final TimeMap<Integer> frameMap = new TimeMap<>();

    protected final CompletionLevel completionLevel;
    protected final J2KReader reader;

    public J2KView(DecodeExecutor _executor, APIRequest _request, DataUri _dataUri) throws Exception {
        super(_executor, _dataUri);
        serial = globalSerial.incrementAndGet();
        request = _request;

        try {
            boolean isJP2 = dataUri.format() == Image.JP2;
            switch (dataUri.format()) {
                case Image.JPIP -> {
                    reader = new J2KReader(dataUri.uri());
                    source = new J2KSource.Remote(reader.getCache());
                }
                case Image.JP2, Image.JPX -> {
                    reader = null;
                    source = new J2KSource.Local(dataUri.file().toString(), isJP2);
                }
                default -> throw new Exception("Unknown image type");
            }
            source.open();

            int[] lut = source.getLUT();
            if (lut != null)
                builtinLUT = new LUT("built-in", lut);

            maxFrame = source.getNumberLayers() - 1;
            metaData = new MetaData[maxFrame + 1];
            xmlMetaData = new String[maxFrame + 1];
            source.extractMetaData(xmlMetaData);
            for (int i = 0; i <= maxFrame; i++) {
                if (xmlMetaData[i] == null) {
                    xmlMetaData[i] = EMPTY_METAXML;
                    metaData[i] = new PixelBasedMetaData(100, 100, dataUri.baseName());
                    Log.warn("Helioviewer metadata missing for layer " + i);
                } else
                    metaData[i] = new XMLMetaDataContainer(xmlMetaData[i]).getHVMetaData();
                frameMap.put(metaData[i].getViewpoint().time, i);
            }
            frameMap.buildIndex();
            if (frameMap.maxIndex() != maxFrame)
                throw new Exception("Duplicated time stamps");

            if (reader == null) {
                completionLevel = new CompletionLevel.Local(source, maxFrame);
            } else {
                completionLevel = new CompletionLevel.Remote(source, maxFrame);

                String[] cacheKey = new String[maxFrame + 1];
                if (request != null) {
                    for (int i = 0; i <= maxFrame; i++) {
                        long milli = frameMap.key(i).milli;
                        if (milli != metaData[i].getViewpoint().time.milli)
                            Log.warn("Badly ordered metadata: " + dataUri + "[" + i + "]: expected " + frameMap.key(i) + ", got " + metaData[i].getViewpoint().time);
                        cacheKey[i] = request.sourceId() + "+" + milli;
                    }
                }
                reader.setCacheKey(cacheKey);
            }
            if (isJP2)
                source.close(); // JP2, close asap

            abolishable = reaper.register(cleanerToken, new J2KAbolisher(serial, reader, source));
        } catch (Exception e) {
            String msg = e instanceof KduException ? "Kakadu error" : e.getMessage();
            throw new Exception(msg + ": " + dataUri, e);
        }
    }

    private record J2KAbolisher(int aSerial, J2KReader aReader, J2KSource aSource) implements Runnable {
        @Override
        public void run() {
            for (DecodeKey key : decodeCache.asMap().keySet()) {
                if (key.params().serial == aSerial)
                    decodeCache.invalidate(key);
            }
            // reader abolish may take too long in stressed conditions
            JHVThread.create(() -> {
                try {
                    if (aReader != null) {
                        aReader.abolish();
                    }
                    aSource.close();
                } catch (KduException e) {
                    Log.error(e);
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
        decodeCache.invalidateAll();
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
            if (frame > completionLevel.getPartialUntil())
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

    protected J2KParams.Decode getDecodeParams(int frame, double pixFactor, float factor) {
        ResolutionSet.Level res;
        if (Movie.isRecording()) { // all bets are off
            res = completionLevel.getResolutionSet(frame).getLevel(0);
            factor = 1;
        } else {
            MetaData m = metaData[frame];
            int reqHeight = (int) (m.getPhysicalRegion().height * pixFactor + .5);
            res = completionLevel.getResolutionSet(frame).getNextLevel(reqHeight, reqHeight);
        }

        return new J2KParams.Decode(serial, frame, res.subImage, res.level, factor);
    }

    private boolean isDecodeComplete(int frame, int level) {
        AtomicBoolean status = completionLevel.getFrameStatus(frame, level); // before signalling to reader
        return status != null && status.get();
    }

    private int currentLevel = 10000;

    protected void signalReader(J2KParams.Decode decodeParams, Position viewpoint, boolean complete) {
        int level = decodeParams.level;
        boolean priority = !Movie.isPlaying();

        if (priority || level < currentLevel) {
            reader.signal(new J2KParams.Read(this, decodeParams, viewpoint, complete, priority));
        }
        currentLevel = level;
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, float factor) {
        J2KParams.Decode decodeParams = getDecodeParams(targetFrame, pixFactor, factor);
        boolean complete = isDecodeComplete(decodeParams.frame, decodeParams.level);
        if (reader != null && !complete) {
            signalReader(decodeParams, viewpoint, complete);
        }
        executeDecode(decodeParams, viewpoint, complete);
    }

    void signalDecoderFromReader(J2KParams.Read readParams) {
        EventQueue.invokeLater(() -> {
            if (readParams.decodeParams.frame == targetFrame) {
                executeDecode(readParams.decodeParams, readParams.viewpoint, readParams.complete);
            }
        });
    }

    private void executeDecode(J2KParams.Decode decodeParams, Position viewpoint, boolean complete) {
        DecodeKey key = new DecodeKey(decodeParams, filterType);
        ImageBuffer imageBuffer = decodeCache.getIfPresent(key);
        if (imageBuffer != null) {
            sendDataToHandler(decodeParams, viewpoint, imageBuffer);
            return;
        }
        int numComps = completionLevel.getResolutionSet(decodeParams.frame).numComps;
        executor.decode(new J2KDecoder(source, decodeParams, numComps, filterType), new J2KCallback(key, viewpoint, complete));
    }

    private class J2KCallback extends DecodeCallback {

        private final DecodeKey key;
        private final Position viewpoint;
        private final boolean complete;

        J2KCallback(DecodeKey _key, Position _viewpoint, boolean _complete) {
            key = _key;
            viewpoint = _viewpoint;
            complete = _complete;
        }

        @Override
        public void onSuccess(ImageBuffer result) {
            if (key.filter() != filterType) return; // filter changed in-flight
            if (complete) decodeCache.put(key, result);

            sendDataToHandler(key.params(), viewpoint, result);
        }

    }

    private void sendDataToHandler(J2KParams.Decode decodeParams, Position viewpoint, ImageBuffer imageBuffer) {
        int frame = decodeParams.frame;
        MetaData m = metaData[frame];
        SubImage roi = decodeParams.subImage;
        ResolutionSet.Level resolution = getResolutionLevel(frame, decodeParams.level);
        Region r = m.roiToRegion(roi.x, roi.y, roi.w, roi.h, resolution.factorX, resolution.factorY);
        ImageData data = new ImageData(imageBuffer, m, r, viewpoint);

        EventQueue.invokeLater(() -> {
            if (dataHandler != null)
                dataHandler.handleData(data);
        });
    }

    @Nullable
    @Override
    public AtomicBoolean getFrameCompletion(int frame) {
        return completionLevel.getFrameStatus(frame, currentLevel);
    }

    @Override
    public boolean isComplete() {
        return completionLevel.isComplete(currentLevel);
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return xmlMetaData[targetFrame];
    }

    public ResolutionSet.Level getResolutionLevel(int frame, int level) {
        return completionLevel.getResolutionSet(frame).getLevel(level);
    }

    CompletionLevel completionLevel() {
        return completionLevel;
    }

}
