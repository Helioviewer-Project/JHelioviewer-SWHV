package org.helioviewer.jhv.timelines.radio;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.j2k.J2KViewCallisto;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;

final class RadioCache {

    private static final int DAYS_IN_CACHE = RadioData.MAX_AMOUNT_OF_DAYS + 4;

    private final RadioData owner;
    private final RadioState state;
    private final DecodeExecutor executor = new DecodeExecutor();
    private final Cache<Long, RadioJ2KData> cache = Caffeine.newBuilder().maximumSize(DAYS_IN_CACHE)
            .removalListener((Long k, RadioJ2KData v, RemovalCause c) -> {
                if (v != null)
                    v.removeData();
            }).build();
    private final HashSet<Long> downloading = new HashSet<>();

    RadioCache(RadioData _owner, RadioState _state) {
        owner = _owner;
        state = _state;
    }

    void invalidateAll() {
        cache.invalidateAll();
    }

    void abolish() {
        invalidateAll();
        executor.abolish();
    }

    void changeColormap() {
        cachedValues().forEach(data -> data.changeColormap(state.colorModel()));
    }

    boolean isDownloading() {
        return !downloading.isEmpty();
    }

    boolean hasData() {
        for (RadioJ2KData data : cachedValues()) {
            if (data.hasImage())
                return true;
        }
        return false;
    }

    void requestVisible(TimeAxis selectedAxis) {
        cachedValues().forEach(data -> data.requestData(selectedAxis));
        requestAndOpenIntervals(selectedAxis.start());
    }

    void forEachData(Consumer<RadioJ2KData> action) {
        cachedValues().forEach(action);
    }

    private Collection<RadioJ2KData> cachedValues() {
        return cache.asMap().values();
    }

    private void requestAndOpenIntervals(long start) {
        long end = Math.min(TimeUtils.floorDay(start) + (DAYS_IN_CACHE - 2) * TimeUtils.DAY_IN_MILLIS, TimeUtils.floorDay(System.currentTimeMillis()));
        for (int i = 0; i < DAYS_IN_CACHE; i++) {
            long date = end - i * TimeUtils.DAY_IN_MILLIS;
            if (!downloading.contains(date) && cache.getIfPresent(date) == null) {
                EDTCallbackExecutor.pool.submit(new RadioJPXDownload(date), new RadioJPXCallback(date));
            }
        }
    }

    private class RadioJPXDownload implements Callable<RadioJ2KData> {

        private final long date;

        RadioJPXDownload(long _date) {
            date = _date;
            downloading.add(date);
            Timelines.getLayers().downloadStarted(owner);
        }

        @Override
        public RadioJ2KData call() throws Exception {
            APIRequest req = new APIRequest("ROB", APIRequest.CallistoID, date, date, APIRequest.CADENCE_ALL);
            DataUri dataUri = NetFileCache.get(new URI(req.toFileRequest()));
            if (dataUri.format() != DataUri.Format.Image.JP2)
                throw new Exception("Invalid data format");

            return new RadioJ2KData(new J2KViewCallisto(executor, req, dataUri), req.startTime(), state);
        }
    }

    private class RadioJPXCallback implements FutureCallback<RadioJ2KData> {

        private final long date;

        RadioJPXCallback(long _date) {
            date = _date;
        }

        private void done() {
            downloading.remove(date);
            Timelines.getLayers().downloadFinished(owner);
        }

        @Override
        public void onSuccess(@Nonnull RadioJ2KData result) {
            done();
            cache.put(date, result);
            result.requestData(DrawController.selectedAxis);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            done();
            Log.error(Throwables.getStackTraceAsString(t));
        }
    }
}
