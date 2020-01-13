package org.helioviewer.jhv.view.j2k;

import java.awt.EventQueue;
import java.io.IOException;
import java.lang.ref.Cleaner;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import kdu_jni.KduException;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.APIResponse;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeMap;
import org.helioviewer.jhv.view.BaseView;
import org.helioviewer.jhv.view.j2k.cache.CacheStatus;
import org.helioviewer.jhv.view.j2k.cache.CacheStatusLocal;
import org.helioviewer.jhv.view.j2k.cache.CacheStatusRemote;
import org.helioviewer.jhv.view.j2k.image.DecodeParams;
import org.helioviewer.jhv.view.j2k.image.ReadParams;
import org.helioviewer.jhv.view.j2k.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.view.j2k.io.jpip.JPIPCache;
import org.helioviewer.jhv.view.j2k.kakadu.KakaduSource;

public class J2KView extends BaseView {

    private static final Cleaner reaper = Cleaner.create();

    private static final int HIRES_CUTOFF = 1280;

    private final int maxFrame;
    private int targetFrame;

    private final long[] cacheKey;
    private final TimeMap<Integer> frameMap = new TimeMap<>();

    private final Cleaner.Cleanable abolishable;
    private final DecodeExecutor executor;
    private final KakaduSource kduSource;
    private final JPIPCache jpipCache;

    protected final CacheStatus cacheStatus;
    protected final J2KReader reader;

    public J2KView(APIRequest _request, APIResponse _response, URI _uri, DecodeExecutor _executor) throws Exception {
        super(_request, _uri);
        executor = _executor == null ? new DecodeExecutor() : _executor;

        long[] frames = _response == null ? null : _response.getFrames();
        if (frames != null) {
            long key = request == null ? 0 : ((long) request.sourceId) << 32;
            int len = frames.length;
            cacheKey = new long[len];
            if (key != 0)
                for (int i = 0; i < len; i++) {
                    cacheKey[i] = key + frames[i];
                }
        } else
            cacheKey = new long[1];

        try {
            String scheme = uri.getScheme().toLowerCase();
            switch (scheme) {
                case "jpip":
                    jpipCache = new JPIPCache();
                    reader = new J2KReader(this);
                    break;
                case "file":
                    jpipCache = null;
                    reader = null;
                    // nothing
                    break;
                default:
                    throw new IOException(scheme + " scheme not supported!");
            }

            kduSource = new KakaduSource(jpipCache, uri);
            maxFrame = kduSource.getNumberLayers() - 1;
            metaData = new MetaData[maxFrame + 1];

            kduSource.extractMetaData(metaData);
            for (int i = 0; i <= maxFrame; i++) {
                frameMap.put(metaData[i].getViewpoint().time, i);
            }
            frameMap.buildIndex();
            if (frameMap.maxIndex() != maxFrame)
                throw new Exception("Duplicated time stamps");
            for (int i = 0; i <= maxFrame; i++) {
                if (frameMap.key(i).milli != metaData[i].getViewpoint().time.milli)
                    throw new Exception("Badly ordered metadata");
            }

            if (frames != null) {
                if (maxFrame + 1 != frames.length)
                    Log.warn(uri + ": expected " + (maxFrame + 1) + "frames, got " + frames.length);
                for (int i = 0; i < Math.min(maxFrame + 1, frames.length); i++) {
                    JHVDate d = frameMap.key(i);
                    if (d.milli != frames[i] * 1000) {
                        cacheKey[i] = 0; // uncacheable
                        Log.warn(uri + "[" + i + "]: expected " + d + ", got " + new JHVDate(frames[i] * 1000));
                    }
                }
            }

            int[] lut = kduSource.getLUT();
            if (lut != null)
                builtinLUT = new LUT(getName() + " built-in", lut);

            if (jpipCache == null)
                cacheStatus = new CacheStatusLocal(kduSource, maxFrame);
            else { // remote
                cacheStatus = new CacheStatusRemote(kduSource, maxFrame);
                reader.start();
            }

            abolishable = reaper.register(this, new Abolisher(executor, reader, jpipCache));
        } catch (KduException e) {
            e.printStackTrace();
            throw new IOException("Failed to create Kakadu machinery: " + e.getMessage(), e);
        }
    }

    long getCacheKey(int frame) {
        return frame < 0 || frame >= cacheKey.length ? 0 : cacheKey[frame];
    }

    String getName() {
        return metaData[0].getDisplayName();
    }

    // if instance was built before cancelling
    private static class Abolisher implements Runnable {

        private final DecodeExecutor aExecutor;
        private final J2KReader aReader;
        private final JPIPCache aJpipCache;

        Abolisher(DecodeExecutor _executor, J2KReader _reader, JPIPCache _jpipCache) {
            aExecutor = _executor;
            aReader = _reader;
            aJpipCache = _jpipCache;
        }

        @Override
        public void run() {
            // executor and reader abolish may take too long in stressed conditions
            new Thread(() -> {
                aExecutor.abolish();
                if (aReader != null) {
                    aReader.abolish();
                }
                try {
                    if (aJpipCache != null) {
                        aJpipCache.Close();
                        aJpipCache.Native_destroy();
                    }
                } catch (KduException e) {
                    e.printStackTrace();
                }
            }).start();
        }

    }

    @Override
    public void abolish() {
        abolishable.clean();
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
    public JHVDate getFirstTime() {
        return frameMap.firstKey();
    }

    @Override
    public JHVDate getLastTime() {
        return frameMap.lastKey();
    }

    @Override
    public boolean setNearestFrame(JHVDate time) {
        int frame = frameMap.nearestValue(time);
        if (frame != targetFrame) {
            if (frame > cacheStatus.getPartialUntil())
                return false;
            targetFrame = frame;
        }
        return true;
    }

    @Override
    public JHVDate getFrameTime(int frame) {
        return frameMap.key(frame);
    }

    @Override
    public JHVDate getNearestTime(JHVDate time) {
        return frameMap.nearestKey(time);
    }

    @Override
    public JHVDate getLowerTime(JHVDate time) {
        return frameMap.lowerKey(time);
    }

    @Override
    public JHVDate getHigherTime(JHVDate time) {
        return frameMap.higherKey(time);
    }

    @Override
    public MetaData getMetaData(JHVDate time) {
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

    @Override
    public void decode(Position viewpoint, double pixFactor, double factor) {
        DecodeParams decodeParams = getDecodeParams(viewpoint, targetFrame, pixFactor, factor);
        if (reader != null && !decodeParams.complete) {
            signalReader(decodeParams);
        }
        executor.decode(decodeParams);
    }

    protected DecodeParams getDecodeParams(Position viewpoint, int frame, double pixFactor, double factor) {
        ResolutionLevel res;
        SubImage subImage;

        if (Movie.isRecording()) { // all bets are off
            res = cacheStatus.getResolutionSet(frame).getResolutionLevel(0);
            subImage = new SubImage(0, 0, res.width, res.height, res.width, res.height);
            factor = 1;
        } else {
            MetaData m = metaData[frame];
            int reqHeight = (int) (m.getPhysicalRegion().height * pixFactor + .5);

            res = cacheStatus.getResolutionSet(frame).getNextResolutionLevel(reqHeight, reqHeight);
            subImage = new SubImage(0, 0, res.width, res.height, res.width, res.height);

            int maxDim = Math.max(res.width, res.height);
            if (maxDim > HIRES_CUTOFF && Movie.isPlaying()) {
                factor = Math.min(factor, 0.5);
            }
        }
        AtomicBoolean status = cacheStatus.getFrameStatus(frame, res.level); // before signalling to reader
        return new DecodeParams(this, viewpoint, status != null && status.get(), subImage, res, frame, factor);
    }

    protected void signalReader(DecodeParams decodeParams) {
        int level = decodeParams.resolution.level;
        boolean priority = !Movie.isPlaying();

        if (priority || level < currentLevel) {
            reader.signalReader(new ReadParams(priority, decodeParams));
        }
        currentLevel = level;
    }

    private int currentLevel = 10000;

    void signalDecoderFromReader(ReadParams params) {
        EventQueue.invokeLater(() -> {
            if (params.decodeParams.frame == targetFrame) {
                // params.decodeParams.complete = true;
                executor.decode(params.decodeParams);
            }
        });
    }

    void setDataFromDecoder(DecodeParams decodeParams, ImageBuffer imageBuffer) {
        MetaData m = metaData[decodeParams.frame];
        Region r = m.roiToRegion(decodeParams.subImage, decodeParams.resolution.factorX, decodeParams.resolution.factorY);
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

    // very slow
    @Nonnull
    @Override
    public String getXMLMetaData() throws Exception {
        return kduSource.extractXMLString(targetFrame);
    }

    public ResolutionLevel getResolutionLevel(int frame, int level) {
        return cacheStatus.getResolutionSet(frame).getResolutionLevel(level);
    }

    int getNumComponents(int frame) {
        return cacheStatus.getResolutionSet(frame).numComps;
    }

    KakaduSource getSource() {
        return kduSource;
    }

    JPIPCache getJPIPCache() {
        return jpipCache;
    }

    CacheStatus getCacheStatus() {
        return cacheStatus;
    }

}
