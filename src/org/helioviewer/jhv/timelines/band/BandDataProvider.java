package org.helioviewer.jhv.timelines.band;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;
import org.helioviewer.jhv.timelines.Timelines;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.util.concurrent.FutureCallback;

public class BandDataProvider {

    private static final ArrayListMultimap<Band, Future<Band.Data>> workers = ArrayListMultimap.create();

    private static void pruneFinished(Band band) {
        workers.get(band).removeIf(Future::isDone);
    }

    static void addDownloads(Band band, List<Interval> intervals) {
        String baseUrl = band.getBandType().getBaseUrl();
        if ("".equals(baseUrl))
            return;
        pruneFinished(band);
        intervals.forEach(interval -> workers.put(band, BandReaderHapi.requestData(baseUrl, interval.start, interval.end)));
    }

    static void stopDownloads(Band band) {
        workers.get(band).forEach(worker -> worker.cancel(true));
        workers.removeAll(band);
    }

    static boolean isDownloadActive(Band band) {
        pruneFinished(band);
        return !workers.get(band).isEmpty();
    }

    public static void loadBand(JSONObject jo) {
        EDTCallbackExecutor.pool.submit(new BandLoad(jo), new BandLoadCallback());
    }

    private record BandLoad(JSONObject jo) implements Callable<BandResponse> {
        @Override
        public BandResponse call() throws Exception {
            return new BandResponse(jo);
        }
    }

    private static class BandLoadCallback implements FutureCallback<BandResponse> {
        @Override
        public void onSuccess(@Nonnull BandResponse result) {
            Band band = Band.createFromType(result.bandType);
            band.addToCache(result.values, result.dates);
            Timelines.getLayers().add(band);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
        }
    }

    private static class BandResponse {

        final BandType bandType;
        final long[] dates;
        final float[] values;

        BandResponse(JSONObject jo) throws Exception {
            JSONObject bo = jo.optJSONObject("bandType");
            if (bo == null)
                throw new Exception("Missing bandType: " + jo);
            bandType = new BandType(bo);

            double multiplier = jo.optDouble("multiplier", 1);
            JSONArray data = jo.optJSONArray("data");
            if (data != null) {
                int len = data.length();
                values = new float[len];
                dates = new long[len];
                for (int i = 0; i < len; i++) {
                    JSONArray entry = data.getJSONArray(i);
                    dates[i] = entry.getLong(0) * 1000L;
                    values[i] = (float) (entry.getDouble(1) * multiplier);
                }
            } else {
                dates = new long[0];
                values = new float[0];
            }
        }

    }

}
