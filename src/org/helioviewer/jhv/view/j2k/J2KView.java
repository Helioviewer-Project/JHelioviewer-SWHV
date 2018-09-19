package org.helioviewer.jhv.view.j2k;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import kdu_jni.KduException;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.APIResponse;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.view.AbstractView;
import org.helioviewer.jhv.view.j2k.cache.CacheStatus;
import org.helioviewer.jhv.view.j2k.cache.CacheStatusLocal;
import org.helioviewer.jhv.view.j2k.cache.CacheStatusRemote;
import org.helioviewer.jhv.view.j2k.image.DecodeParams;
import org.helioviewer.jhv.view.j2k.image.ImageParams;
import org.helioviewer.jhv.view.j2k.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.view.j2k.io.jpip.JPIPCache;
import org.helioviewer.jhv.view.j2k.kakadu.KakaduSource;

// This class is responsible for reading and decoding of JPEG2000 images
public class J2KView extends AbstractView {

    private static final int HIRES_CUTOFF = 1280;

    private int targetFrame = 0;
    private int trueFrame = 0;

    private int fps;
    private int fpsCount;
    private long fpsTime = System.currentTimeMillis();

    private final long cacheKey[];

    private final DecodeExecutor executor = new DecodeExecutor();
    private final KakaduSource kduSource;
    private JPIPCache jpipCache;

    protected final CacheStatus cacheStatus;
    protected J2KReader reader;

    public J2KView(URI _uri, APIRequest _request, APIResponse _response) throws Exception {
        super(_uri, _request);

        long frames[] = _response == null ? null : _response.getFrames();
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
                if (metaData[i] == null)
                    metaData[i] = new PixelBasedMetaData(256, 256, i); // tbd real size
            }

            if (frames != null) {
                if (maxFrame + 1 != frames.length)
                    Log.warn(uri + ": expected " + (maxFrame + 1) + "frames, got " + frames.length);
                for (int i = 0; i < Math.min(maxFrame + 1, frames.length); i++) {
                    JHVDate d = getFrameTime(i);
                    if (d.milli != frames[i] * 1000) {
                        cacheKey[i] = 0; // uncacheable
                        Log.warn(uri + "[" + i + "]: expected " + d + ", got " + new JHVDate(frames[i]));
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
        } catch (KduException e) {
            e.printStackTrace();
            throw new IOException("Failed to create Kakadu machinery: " + e.getMessage(), e);
        }
    }

    long getCacheKey(int frame) {
        return frame < 0 || frame >= cacheKey.length ? 0 : cacheKey[frame];
    }

    // if instance was built before cancelling
    @Override
    protected void finalize() throws Throwable {
        try {
            abolish();
        } finally {
            super.finalize();
        }
    }

    private volatile boolean isAbolished = false;

    @Override
    public void abolish() {
        if (isAbolished)
            return;
        isAbolished = true;

        new Thread(() -> {
            executor.abolish();
            if (reader != null) {
                reader.abolish();
                reader = null;
            }
            kduDestroy();
        }).start();
    }

    private void kduDestroy() {
        try {
            if (jpipCache != null) {
                jpipCache.Close();
                jpipCache.Native_destroy();
            }
        } catch (KduException e) {
            e.printStackTrace();
        } finally {
            jpipCache = null;
        }
    }

    @Override
    public int getCurrentFramerate() {
        long currentTime = System.currentTimeMillis();
        long delta = currentTime - fpsTime;

        if (delta > 1000) {
            fps = (int) (1000 * fpsCount / (double) delta + .5);
            fpsCount = 0;
            fpsTime = currentTime;
        }
        return fps;
    }

    @Override
    public int getCurrentFrameNumber() {
        return targetFrame;
    }

    // to be accessed only from Layers
    @Nullable
    @Override
    public JHVDate getNextTime(AnimationMode mode, int deltaT) {
        int next = targetFrame + 1;
        switch (mode) {
            case Stop:
                if (next > maxFrame) {
                    return null;
                }
                break;
            case Swing:
                if (targetFrame == maxFrame) {
                    Movie.setAnimationMode(AnimationMode.SwingDown);
                    return metaData[targetFrame - 1].getViewpoint().time;
                }
                break;
            case SwingDown:
                if (targetFrame == 0) {
                    Movie.setAnimationMode(AnimationMode.Swing);
                    return metaData[1].getViewpoint().time;
                }
                return metaData[targetFrame - 1].getViewpoint().time;
            default: // Loop
                if (next > maxFrame) {
                    return metaData[0].getViewpoint().time;
                }
        }
        return metaData[next].getViewpoint().time;
    }

    @Override
    public void setFrame(JHVDate time) {
        int frame = getFrameNumber(time);
        if (frame != targetFrame) {
            if (frame > cacheStatus.getPartialUntil())
                return;
            targetFrame = frame;
        }
    }

    private int getFrameNumber(JHVDate time) {
        int frame = -1;
        long lastDiff, currentDiff = -Long.MAX_VALUE;
        do {
            lastDiff = currentDiff;
            currentDiff = metaData[++frame].getViewpoint().time.milli - time.milli;
        } while (currentDiff < 0 && frame < maxFrame);
        return -lastDiff < currentDiff ? frame - 1 : frame;
    }

    @Override
    public JHVDate getFrameTime(int frame) {
        if (frame < 0) {
            frame = 0;
        } else if (frame > maxFrame) {
            frame = maxFrame;
        }
        return metaData[frame].getViewpoint().time;
    }

    @Override
    public JHVDate getFrameTime(JHVDate time) {
        return metaData[getFrameNumber(time)].getViewpoint().time;
    }

    @Override
    public MetaData getMetaData(JHVDate time) {
        return metaData[getFrameNumber(time)];
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
    public void decode(int serialNo, double pixFactor, double factor) {
        DecodeParams decodeParams = getDecodeParams(serialNo, targetFrame, pixFactor, factor);
        if (reader != null && !decodeParams.complete) {
            signalReader(decodeParams);
        }
        executor.execute(this, decodeParams);
    }

    protected DecodeParams getDecodeParams(int serialNo, int frame, double pixFactor, double factor) {
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
        return new DecodeParams(serialNo, status != null && status.get(), subImage, res, frame, factor);
    }

    protected void signalReader(DecodeParams decodeParams) {
        int level = decodeParams.resolution.level;
        boolean priority = !Movie.isPlaying();

        if (priority || level < currentLevel) {
            reader.signalReader(new ImageParams(priority, decodeParams));
        }
        currentLevel = level;
    }

    private int currentLevel = 10000;

    void signalDecoderFromReader(ImageParams params) {
        if (isAbolished)
            return;
        EventQueue.invokeLater(() -> {
            if (params.decodeParams.frame == targetFrame) {
                // params.decodeParams.complete = true;
                executor.execute(this, params.decodeParams);
            }
        });
    }

    void setDataFromDecoder(DecodeParams decodeParams, ImageBuffer imageBuffer) {
        if (isAbolished)
            return;

        int frame = decodeParams.frame;
        if (frame != trueFrame) {
            trueFrame = frame;
            ++fpsCount;
        }

        executor.addToCache(decodeParams, imageBuffer);

        ImageData data = new ImageData(imageBuffer);
        data.setSerial(decodeParams.serialNo);

        MetaData m = metaData[frame];
        data.setMetaData(m);
        data.setRegion(m.roiToRegion(decodeParams.subImage, decodeParams.resolution.factorX, decodeParams.resolution.factorY));

        EventQueue.invokeLater(() -> {
            if (dataHandler != null)
                dataHandler.handleData(data);
        });
    }

    KakaduSource getSource() {
        return kduSource;
    }

    @Override
    public AtomicBoolean getFrameCacheStatus(int frame) {
        return cacheStatus.getFrameStatus(frame, currentLevel);
    }

    @Override
    public boolean isComplete() {
        return cacheStatus.isComplete(currentLevel);
    }

    public ResolutionLevel getResolutionLevel(int frame, int level) {
        return cacheStatus.getResolutionSet(frame).getResolutionLevel(level);
    }

    int getNumComponents(int frame) {
        return cacheStatus.getResolutionSet(frame).numComps;
    }

    JPIPCache getJPIPCache() {
        return jpipCache;
    }

    CacheStatus getCacheStatus() {
        return cacheStatus;
    }

    // very slow
    @Override
    public String getXMLMetaData() throws Exception {
        return kduSource.extractXMLString(trueFrame);
    }

}
