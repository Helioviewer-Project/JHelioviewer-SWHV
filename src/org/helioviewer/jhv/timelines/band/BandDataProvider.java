package org.helioviewer.jhv.timelines.band;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.TimelineSettings;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.util.concurrent.FutureCallback;

public class BandDataProvider {

    private static final ArrayListMultimap<Band, Future<BandResponse>> workerMap = ArrayListMultimap.create();

    public static void loadBandTypes() {
        EventQueueCallbackExecutor.pool.submit(new BandTypeDownload(), new BandTypeDownloadCallback());
    }

    public static void loadBand(JSONObject jo) {
        EventQueueCallbackExecutor.pool.submit(new BandLoad(jo), new BandLoadCallback());
    }

    static void addDownloads(Band band, List<Interval> intervals) {
        for (Interval interval : intervals) {
            Future<BandResponse> worker = EventQueueCallbackExecutor.pool.submit(
                    new BandDownload(band, interval.start, interval.end), new BandDownloadCallback(band));
            workerMap.put(band, worker);
        }
    }

    static void stopDownloads(Band band) {
        workerMap.get(band).forEach(worker -> worker.cancel(true));
        workerMap.removeAll(band);
    }

    static boolean isDownloadActive(Band band) {
        for (Future<BandResponse> worker : workerMap.get(band)) {
            if (!worker.isDone())
                return true;
        }
        return false;
    }

    private record BandDownload(Band band, long startTime, long endTime) implements Callable<BandResponse> {

        BandDownload {
            Timelines.getLayers().downloadStarted(band);
        }

        @Override
        public BandResponse call() throws Exception {
            BandType type = band.getBandType();
            String request = type.getBaseURL() +
                    "start_date=" + TimeUtils.formatDate(startTime) +
                    "&end_date=" + TimeUtils.formatDate(endTime) +
                    "&timeline=" + type.getName();
            return new BandResponse(JSONUtils.get(request));
        }

    }

    private record BandDownloadCallback(Band band) implements FutureCallback<BandResponse> {

        @Override
        public void onSuccess(BandResponse result) {
            if (result.bandName.equals(band.getBandType().getName()))
                band.addToCache(result.values, result.dates);
            else
                Log.error("Expected " + band.getBandType().getName() + ", got " + result.bandName);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("Error downloading band", t);
        }

    }

    private record BandLoad(JSONObject jo) implements Callable<BandResponse> {
        @Override
        public BandResponse call() {
            return new BandResponse(jo);
        }
    }

    private static class BandLoadCallback implements FutureCallback<BandResponse> {

        @Override
        public void onSuccess(BandResponse result) {
            Band band = new Band(result.bandType == null ? BandType.getBandType(result.bandName) : result.bandType);
            band.addToCache(result.values, result.dates);
            Timelines.getLayers().add(band);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("Error loading band", t);
        }

    }

    private static class BandTypeDownload implements Callable<JSONArray> {
        @Override
        public JSONArray call() throws Exception {
            return JSONUtils.get(TimelineSettings.baseURL).getJSONArray("objects");
        }
    }

    private static class BandTypeDownloadCallback implements FutureCallback<JSONArray> {

        @Override
        public void onSuccess(JSONArray result) {
            BandType.loadBandTypes(result);
            Timelines.td.getObservationPanel().setupDatasets();
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("Error downloading band types", t);
        }

    }

}
