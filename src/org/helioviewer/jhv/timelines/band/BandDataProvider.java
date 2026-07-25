package org.helioviewer.jhv.timelines.band;

import java.util.concurrent.Callable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.Timelines;

import org.json.JSONArray;
import org.json.JSONObject;

public class BandDataProvider {

    public static void loadBand(JSONObject jo) {
        Task.submit("band", new BandLoad(jo), BandDataProvider::acceptData, BandDataProvider::onFailure);
    }

    static void acceptData(Band.Data line) {
        TimelineLayers layers = Timelines.getLayers();
        Band band = layers.addBand(line.bandType());
        boolean hasDataChanged = band.addToCache(line.values(), line.dates());
        if (hasDataChanged)
            layers.updateRow(band);
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
