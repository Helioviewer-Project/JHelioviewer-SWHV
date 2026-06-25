package org.helioviewer.jhv.view.j2k;

import java.awt.EventQueue;
import java.lang.ref.Cleaner;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageBufferCache;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.DataUri.Format.Image;
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
import org.helioviewer.jhv.view.View;

import kdu_jni.KduException;

public class J2KView extends BaseView {

    private static final AtomicInteger globalSerial = new AtomicInteger();

    private static final Cleaner reaper = Cleaner.create();
    private final Cleaner.Cleanable abolishable;
    @SuppressWarnings("FieldCanBeLocal")
    // Cleaner tracks reachability of this token; the cleanup action must not capture this J2KView.
    private final Object cleanerToken = new Object();

    private final APIRequest request;
    protected final int serial;

    private final J2KSource source;
    private final int maxFrame;
    private int targetFrame;

    private final String[] xmlMetaData;
    private final TimeMap<Integer> frameMap = new TimeMap<>();

    protected final J2KReader reader;

    public J2KView(LatestWorker<ImageBuffer> _executor, APIRequest _request, DataUri _dataUri) throws Exception {
        super(_executor, _dataUri);
        serial = globalSerial.incrementAndGet();
        request = _request;

        try {
            boolean isJP2 = dataUri.format() == Image.JP2;
            switch (dataUri.format()) {
                case Image.JPIP -> {
                    J2KSource.Remote remote = new J2KSource.Remote();
                    reader = new J2KReader(dataUri.uri(), remote);
                    source = remote;
                }
                case Image.JP2, Image.JPX -> {
                    reader = null;
                    source = new J2KSource.Local(dataUri.file().toString(), isJP2);
                }
                default -> throw new Exception("Unknown image type");
            }
            source.open();

            LUT lut = source.getLUT();
            if (lut != null)
                builtinLUT = lut;

            maxFrame = source.maxFrame();
            metaData = new MetaData[maxFrame + 1];
            xmlMetaData = new String[maxFrame + 1];
            source.extractMetaData(xmlMetaData);
            for (int i = 0; i <= maxFrame; i++) {
                try {
                    if (xmlMetaData[i] == null)
                        throw new Exception("Missing XML metadata");
                    metaData[i] = new FitsMetaData(new XMLMetaDataContainer(xmlMetaData[i]));
                } catch (Exception e) {
                    xmlMetaData[i] = EMPTY_METAXML;
                    ResolutionSet.Level level = source.readResolutionSet(i).getLevel(0);
                    metaData[i] = new BasicMetaData(level.width(), level.height(), dataUri.baseName());
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
                    aSource.closeWhenUnused();
                    aSource.destroy();
                    clearCache(aSerial);
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

    protected J2KParams.Decode getDecodeParams(int frame, double pixFactor, float factor) {
        ResolutionSet.Level res;
        if (ExportMovie.isRecording()) { // all bets are off
            res = source.resolutionSet(frame).getLevel(0);
            factor = 1;
        } else {
            MetaData m = metaData[frame];
            int reqHeight = (int) (m.getPhysicalRegion().height * pixFactor + .5);
            res = source.resolutionSet(frame).getNextLevel(reqHeight, reqHeight);
        }

        return new J2KParams.Decode(frame, res.subImage(), res.level(), factor);
    }

    private int currentLevel = 10000;

    protected void signalReader(J2KParams.Decode decodeParams, Position viewpoint) {
        int level = decodeParams.level;
        boolean priority = !Player.isPlaying();

        if (priority || level < currentLevel) {
            reader.signal(new J2KParams.Read(this, (J2KSource.Remote) source, decodeParams, viewpoint, priority));
            currentLevel = level;
        }
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, float factor) {
        J2KParams.Decode decodeParams = getDecodeParams(targetFrame, pixFactor, factor);
        AtomicBoolean status = source.getFrameStatus(decodeParams.frame, decodeParams.level); // before signalling to reader
        boolean cacheResult = status != null && status.get();
        if (reader != null && !cacheResult) {
            signalReader(decodeParams, viewpoint);
        }

        J2KDecodeKey key = new J2KDecodeKey(serial, decodeParams, filterType);
        ImageBuffer imageBuffer = ImageBufferCache.get(key);
        if (imageBuffer != null) {
            // Mark running decodes stale before publishing this cached result.
            executor.cancel();
            sendDataToHandler(decodeParams, viewpoint, imageBuffer);
            return;
        }
        submitDecode(decodeParams, viewpoint, cacheResult);
    }

    void refreshDecodeFromReader(J2KParams.Decode decodeParams, Position viewpoint) {
        EventQueue.invokeLater(() -> {
            if (decodeParams.frame == targetFrame) {
                submitDecode(decodeParams, viewpoint, true);
            }
        });
    }

    private void submitDecode(J2KParams.Decode decodeParams, Position viewpoint, boolean cacheResult) {
        J2KDecodeKey key = new J2KDecodeKey(serial, decodeParams, filterType);
        int numComps = source.resolutionSet(decodeParams.frame).numComps;
        try {
            executor.submit(new J2KDecoder(source, decodeParams, numComps, filterType, filterRegion(decodeParams)), new J2KCallback(key, viewpoint, cacheResult));
        } catch (RejectedExecutionException ignore) {
            // Teardown may shut the executor down before a late refresh/resubmit reaches this point.
        }
    }

    private Region filterRegion(J2KParams.Decode decodeParams) {
        int frame = decodeParams.frame;
        J2KParams.SubImage roi = decodeParams.subImage;
        ResolutionSet.Level resolution = getResolutionLevel(frame, decodeParams.level);
        return metaData[frame].roiToSunRegion(roi.x(), roi.y(), roi.w(), roi.h(), resolution.factorX(), resolution.factorY());
    }

    private class J2KCallback implements LatestWorker.Callback<ImageBuffer> {

        private final J2KDecodeKey key;
        private final Position viewpoint;
        private final boolean cacheResult;

        J2KCallback(J2KDecodeKey _key, Position _viewpoint, boolean _cacheResult) {
            key = _key;
            viewpoint = _viewpoint;
            cacheResult = _cacheResult;
        }

        @Override
        public void onSuccess(ImageBuffer result, boolean fresh) {
            if (key.filter() != filterType) return; // filter changed in-flight
            if (cacheResult) ImageBufferCache.put(key, result);

            // This decode was superseded after it started; do not publish it to the layer.
            if (!fresh) return;
            sendDataToHandler(key.params(), viewpoint, result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t, boolean fresh) {
            Log.errorStack(t);
        }

    }

    private void sendDataToHandler(J2KParams.Decode decodeParams, Position viewpoint, ImageBuffer imageBuffer) {
        imageBuffer.protectFromExplicitFree();
        int frame = decodeParams.frame;
        MetaData m = metaData[frame];
        J2KParams.SubImage roi = decodeParams.subImage;
        ResolutionSet.Level resolution = getResolutionLevel(frame, decodeParams.level);
        Region r = m.roiToRegion(roi.x(), roi.y(), roi.w(), roi.h(), resolution.factorX(), resolution.factorY());

        View.ImageData data = new View.ImageData(imageBuffer, m, r, viewpoint);
        EventQueue.invokeLater(() -> {
            if (dataHandler != null)
                dataHandler.handleData(data);
            else
                imageBuffer.allowExplicitFree();
        });
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

    public ResolutionSet.Level getResolutionLevel(int frame, int level) {
        return source.resolutionSet(frame).getLevel(level);
    }

}
