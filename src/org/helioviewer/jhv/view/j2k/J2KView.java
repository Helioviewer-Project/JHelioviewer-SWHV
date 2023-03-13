package org.helioviewer.jhv.view.j2k;

import java.awt.EventQueue;
import java.lang.ref.Cleaner;
import java.net.URI;
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
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;
import org.helioviewer.jhv.view.BaseView;
import org.helioviewer.jhv.view.DecodeCallback;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.j2k.cache.CacheStatus;
import org.helioviewer.jhv.view.j2k.cache.CacheStatusLocal;
import org.helioviewer.jhv.view.j2k.cache.CacheStatusRemote;
import org.helioviewer.jhv.view.j2k.image.DecodeParams;
import org.helioviewer.jhv.view.j2k.image.ReadParams;
import org.helioviewer.jhv.view.j2k.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.view.j2k.image.SubImage;
import org.helioviewer.jhv.view.j2k.io.jpip.JPIPCache;
import org.helioviewer.jhv.view.j2k.kakadu.KakaduSource;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class J2KView extends BaseView {

    private static final AtomicInteger global_serial = new AtomicInteger(0);

    private static final Cache<DecodeParams, ImageBuffer> decodeCache = Caffeine.newBuilder().softValues().build();

    private static final Cleaner reaper = Cleaner.create();
    private final Cleaner.Cleanable abolishable;

    private final int maxFrame;
    private int targetFrame;
    private final String[] xmlMetaData;

    private final String[] cacheKey;
    private final TimeMap<Integer> frameMap = new TimeMap<>();

    private final KakaduSource source;
    private final JPIPCache jpipCache;

    protected final int serial;
    protected final CacheStatus cacheStatus;
    protected final J2KReader reader;

    private static int incrementSerial() {
        while (true) {
            int existingValue = global_serial.get();
            int newValue = existingValue + 1;
            if (global_serial.compareAndSet(existingValue, newValue)) {
                return newValue;
            }
        }
    }

    public J2KView(DecodeExecutor _executor, APIRequest _request, URI _uri) throws Exception {
        super(_executor, _request, _uri);
        serial = incrementSerial();

        try {
            String scheme = uri.getScheme().toLowerCase();
            switch (scheme) {
                case "jpip" -> {
                    jpipCache = new JPIPCache();
                    reader = new J2KReader(this);
                }
                case "file" -> {
                    jpipCache = null;
                    reader = null;
                }
                default -> throw new Exception(scheme + " scheme not supported!");
            }

            source = new KakaduSource(jpipCache, uri);
            maxFrame = source.getNumberLayers() - 1;

            xmlMetaData = source.extractMetaData();
            metaData = new MetaData[maxFrame + 1];
            for (int i = 0; i <= maxFrame; i++) {
                if (xmlMetaData[i] == null) {
                    xmlMetaData[i] = "<meta/>";
                    metaData[i] = new PixelBasedMetaData(100, 100, uri);
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
                        Log.warn("Badly ordered metadata: " + uri + "[" + i + "]: expected " + frameMap.key(i) + ", got " + metaData[i].getViewpoint().time);

                    cacheKey[i] = request.sourceId() + "+" + milli;
                }
            }

            int[] lut = source.getLUT();
            if (lut != null)
                builtinLUT = new LUT(getName() + " built-in", lut);

            if (jpipCache == null) {
                cacheStatus = new CacheStatusLocal(source, maxFrame);
            } else { // remote
                cacheStatus = new CacheStatusRemote(source, maxFrame);
                reader.start();
            }

            abolishable = reaper.register(this, new J2KAbolisher(serial, reader, source, jpipCache));
        } catch (Exception e) {
            String msg = e instanceof KduException ? "Kakadu error" : e.getMessage();
            throw new Exception(msg + ": " + uri, e);
        }
    }

    @Nullable
    String getCacheKey(int frame) {
        return frame < 0 || frame >= cacheKey.length ? null : cacheKey[frame];
    }

    String getName() {
        return metaData[0].getDisplayName();
    }

    private record J2KAbolisher(int aSerial, J2KReader aReader, KakaduSource aSource,
                                JPIPCache aJpipCache) implements Runnable {
        @Override
        public void run() {
            for (DecodeParams params : decodeCache.asMap().keySet()) {
                if (params.serial == aSerial)
                    decodeCache.invalidate(params);
            }
            // reader abolish may take too long in stressed conditions
            new Thread(() -> {
                if (aReader != null) {
                    aReader.abolish();
                }
                try {
                    if (aSource != null) {
                        aSource.abolish();
                    }
                    if (aJpipCache != null) {
                        aJpipCache.Close();
                        aJpipCache.Native_destroy();
                    }
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
            if (frame > cacheStatus.getPartialUntil())
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

    protected DecodeParams getDecodeParams(Position viewpoint, int frame, double pixFactor, float factor) {
        ResolutionLevel res;
        if (Movie.isRecording()) { // all bets are off
            res = cacheStatus.getResolutionSet(frame).getResolutionLevel(0);
            factor = 1;
        } else {
            MetaData m = metaData[frame];
            int reqHeight = (int) (m.getPhysicalRegion().height * pixFactor + .5);
            res = cacheStatus.getResolutionSet(frame).getNextResolutionLevel(reqHeight, reqHeight);
        }

        AtomicBoolean status = cacheStatus.getFrameStatus(frame, res.level); // before signalling to reader
        return new DecodeParams(serial, frame, res.subImage, res.level, factor, status != null && status.get(), viewpoint);
    }

    private int currentLevel = 10000;

    protected void signalReader(DecodeParams decodeParams) {
        int level = decodeParams.level;
        boolean priority = !Movie.isPlaying();

        if (priority || level < currentLevel) {
            reader.signalReader(new ReadParams(this, decodeParams, priority));
        }
        currentLevel = level;
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, float factor) {
        DecodeParams decodeParams = getDecodeParams(viewpoint, targetFrame, pixFactor, factor);
        if (reader != null && !decodeParams.complete) {
            signalReader(decodeParams);
        }
        executeDecode(decodeParams);
    }

    void signalDecoderFromReader(ReadParams params) {
        EventQueue.invokeLater(() -> {
            if (params.decodeParams.frame == targetFrame) {
                // params.decodeParams.complete = true;
                executeDecode(params.decodeParams);
            }
        });
    }

    private void executeDecode(DecodeParams params) {
        ImageBuffer imageBuffer = decodeCache.getIfPresent(params);
        if (imageBuffer == null) {
            executor.decode(new J2KDecoder(this, params, mgn), new J2KCallback(params));
        } else {
            sendDataToHandler(params, imageBuffer);
        }
    }

    private class J2KCallback extends DecodeCallback {

        private final DecodeParams params;

        J2KCallback(DecodeParams _params) {
            params = _params;
        }

        @Override
        public void onSuccess(ImageBuffer result) {
            if (params.complete)
                decodeCache.put(params, result);
            sendDataToHandler(params, result);
        }

    }

    private void sendDataToHandler(DecodeParams decodeParams, ImageBuffer imageBuffer) {
        int frame = decodeParams.frame;
        MetaData m = metaData[frame];
        SubImage roi = decodeParams.subImage;
        ResolutionLevel resolution = getResolutionLevel(frame, decodeParams.level);
        Region r = m.roiToRegion(roi.x, roi.y, roi.w, roi.h, resolution.factorX, resolution.factorY);
        ImageData data = new ImageData(imageBuffer, m, r, decodeParams.viewpoint);

        EventQueue.invokeLater(() -> {
            if (dataHandler != null)
                dataHandler.handleData(data);
        });
    }

    @Nullable
    @Override
    public AtomicBoolean getFrameCacheStatus(int frame) {
        return cacheStatus.getFrameStatus(frame, currentLevel);
    }

    @Override
    public boolean isComplete() {
        return cacheStatus.isComplete(currentLevel);
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return xmlMetaData[targetFrame];
    }

    public ResolutionLevel getResolutionLevel(int frame, int level) {
        return cacheStatus.getResolutionSet(frame).getResolutionLevel(level);
    }

    int getNumComponents(int frame) {
        return cacheStatus.getResolutionSet(frame).numComps;
    }

    KakaduSource getSource() {
        return source;
    }

    JPIPCache getJPIPCache() {
        return jpipCache;
    }

    CacheStatus getCacheStatus() {
        return cacheStatus;
    }

}
