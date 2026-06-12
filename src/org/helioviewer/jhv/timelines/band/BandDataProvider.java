package org.helioviewer.jhv.timelines.band;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.timelines.Timelines;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.ArrayListMultimap;

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
        intervals.forEach(interval -> workers.put(band, BandReaderHapi.requestData(baseUrl, interval.start(), interval.end())));
        Timelines.getLayers().updateRow(band);
    }

    static void stopDownloads(Band band) {
        workers.get(band).forEach(worker -> worker.cancel(true));
        workers.removeAll(band);
        Timelines.getLayers().updateRow(band);
    }

    static boolean isDownloadActive(Band band) {
        pruneFinished(band);
        return !workers.get(band).isEmpty();
    }

    public static void loadBand(JSONObject jo) {
        Task.submit("band", new BandLoad(jo), BandDataProvider::acceptData, BandDataProvider::onFailure);
    }

    static void acceptData(Band.Data line) {
        Band band = Band.createFromType(line.bandType());
        boolean hasDataChanged = band.addToCache(line.values(), line.dates());
        Timelines.getLayers().add(band);
        if (hasDataChanged)
            Timelines.getLayers().updateRow(band);
    }

    private record BandLoad(JSONObject jo) implements Callable<Band.Data> {
        @Override
        public Band.Data call() throws Exception {
            JSONObject bo = jo.optJSONObject("bandType");
            if (bo == null)
                throw new Exception("Missing bandType: " + jo);
            BandType bandType = new BandType(bo);

            double multiplier = jo.optDouble("multiplier", 1);
            JSONArray data = jo.optJSONArray("data");
            long[] dates;
            float[] values;
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
            return new Band.Data(bandType, dates, values);
        }
    }

    private static void onFailure(String ignoredLogContext, Throwable t) {
        Log.error(t);
    }

}
