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
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;
import org.helioviewer.jhv.view.BaseView;
import org.helioviewer.jhv.view.DecodeCallback;
import org.helioviewer.jhv.view.DecodeExecutor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class J2KView extends BaseView {

    private static final AtomicInteger global_serial = new AtomicInteger(0);

    private static final Cache<J2KParams.Decode, ImageBuffer> decodeCache = Caffeine.newBuilder().softValues().build();

    private static final Cleaner reaper = Cleaner.create();
    private final Cleaner.Cleanable abolishable;

    private final int maxFrame;
    private int targetFrame;
    private final String[] xmlMetaData;

    private final String[] cacheKey;
    private final TimeMap<Integer> frameMap = new TimeMap<>();

    private final J2KSource source;

    protected final int serial;
    protected final CompletionLevel completionLevel;
    protected final J2KReader reader;
    private final boolean isJP2;

    private static int incrementSerial() {
        while (true) {
            int existingValue = global_serial.get();
            int newValue = existingValue + 1;
            if (global_serial.compareAndSet(existingValue, newValue)) {
                return newValue;
            }
        }
    }

    public J2KView(DecodeExecutor _executor, APIRequest _request, DataUri _dataUri) throws Exception {
        super(_executor, _request, _dataUri);
        serial = incrementSerial();

        try {
            switch (dataUri.format()) {
                case JPIP -> {
                    reader = new J2KReader(dataUri.uri());
                    source = new J2KSource.Remote(reader.getCache());
                }
                case JP2, JPX -> {
                    reader = null;
                    source = new J2KSource.Local(dataUri.file().toString());
                }
                default -> throw new Exception("Unknown image type");
            }
            isJP2 = dataUri.format() == DataUri.Format.JP2;
            source.open();

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

            cacheKey = new String[maxFrame + 1];
            if (request != null) {
                for (int i = 0; i <= maxFrame; i++) {
                    long milli = frameMap.key(i).milli;
                    if (milli != metaData[i].getViewpoint().time.milli)
                        Log.warn("Badly ordered metadata: " + dataUri + "[" + i + "]: expected " + frameMap.key(i) + ", got " + metaData[i].getViewpoint().time);

                    cacheKey[i] = request.sourceId() + "+" + milli;
                }
            }

            int[] lut = source.getLUT();
            if (lut != null)
                builtinLUT = new LUT("built-in", lut);

            completionLevel = reader == null ? new CompletionLevel.Local(source, maxFrame) : new CompletionLevel.Remote(source, maxFrame);
            if (isJP2)
                source.close(); // JP2, close asap

            abolishable = reaper.register(this, new J2KAbolisher(serial, reader, source));
        } catch (Exception e) {
            String msg = e instanceof KduException ? "Kakadu error" : e.getMessage();
            throw new Exception(msg + ": " + dataUri, e);
        }
    }

    @Nullable
    String getCacheKey(int frame) {
        return frame < 0 || frame >= cacheKey.length ? null : cacheKey[frame];
    }

    private record J2KAbolisher(int aSerial, J2KReader aReader, J2KSource aSource) implements Runnable {
        @Override
        public void run() {
            for (J2KParams.Decode params : decodeCache.asMap().keySet()) {
                if (params.serial == aSerial)
                    decodeCache.invalidate(params);
            }
            // reader abolish may take too long in stressed conditions
            new Thread(() -> {
                try {
                    if (aReader != null) {
                        aReader.abolish();
                    }
                    aSource.close();
                } catch (KduException e) {
                    Log.error(e);
                }
            }).start();
        }
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

    protected J2KParams.Decode getDecodeParams(Position viewpoint, int frame, double pixFactor, float factor) {
        ResolutionSet.Level res;
        if (Movie.isRecording()) { // all bets are off
            res = completionLevel.getResolutionSet(frame).getLevel(0);
            factor = 1;
        } else {
            MetaData m = metaData[frame];
            int reqHeight = (int) (m.getPhysicalRegion().height * pixFactor + .5);
            res = completionLevel.getResolutionSet(frame).getNextLevel(reqHeight, reqHeight);
        }

        AtomicBoolean status = completionLevel.getFrameStatus(frame, res.level); // before signalling to reader
        return new J2KParams.Decode(serial, frame, res.subImage, res.level, factor, status != null && status.get(), viewpoint);
    }

    private int currentLevel = 10000;

    protected void signalReader(J2KParams.Decode decodeParams) {
        int level = decodeParams.level;
        boolean priority = !Movie.isPlaying();

        if (priority || level < currentLevel) {
            reader.signal(new J2KParams.Read(this, decodeParams, priority));
        }
        currentLevel = level;
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, float factor) {
        J2KParams.Decode decodeParams = getDecodeParams(viewpoint, targetFrame, pixFactor, factor);
        if (reader != null && !decodeParams.complete) {
            signalReader(decodeParams);
        }
        executeDecode(decodeParams);
    }

    void signalDecoderFromReader(J2KParams.Read readParams) {
        EventQueue.invokeLater(() -> {
            if (readParams.decodeParams.frame == targetFrame) {
                executeDecode(readParams.decodeParams);
            }
        });
    }

    private void executeDecode(J2KParams.Decode decodeParams) {
        ImageBuffer imageBuffer = decodeCache.getIfPresent(decodeParams);
        if (imageBuffer == null) {
            int numComps = completionLevel.getResolutionSet(decodeParams.frame).numComps;
            try {
                executor.decode(new J2KDecoder(source().jpxSource(), decodeParams, numComps, mgn), new J2KCallback(decodeParams));
            } catch (Exception e) {
                Log.error(e);
            }
        } else {
            sendDataToHandler(decodeParams, imageBuffer);
        }
    }

    private class J2KCallback extends DecodeCallback {

        private final J2KParams.Decode params;

        J2KCallback(J2KParams.Decode _params) {
            params = _params;
        }

        @Override
        public void onSuccess(ImageBuffer result) {
            if (params.complete) {
                decodeCache.put(params, result);
                if (isJP2) { // JP2, close asap
                    try {
                        source.close();
                    } catch (KduException e) {
                        Log.error(e);
                    }
                }
            }
            sendDataToHandler(params, result);
        }

    }

    private void sendDataToHandler(J2KParams.Decode decodeParams, ImageBuffer imageBuffer) {
        int frame = decodeParams.frame;
        MetaData m = metaData[frame];
        SubImage roi = decodeParams.subImage;
        ResolutionSet.Level resolution = getResolutionLevel(frame, decodeParams.level);
        Region r = m.roiToRegion(roi.x, roi.y, roi.w, roi.h, resolution.factorX, resolution.factorY);
        ImageData data = new ImageData(imageBuffer, m, r, decodeParams.viewpoint);

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

    J2KSource source() throws KduException {
        if (isJP2) { // JP2, reopen
            source.open();
        }
        return source;
    }

    CompletionLevel completionLevel() {
        return completionLevel;
    }

}
