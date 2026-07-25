package org.helioviewer.jhv.timelines.band;

import java.awt.EventQueue;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.draw.DrawController;

import org.json.JSONArray;
import org.json.JSONObject;

public final class BandImporter {

    public static void loadBand(JSONObject jo) {
        Task.submit("band", new BandLoad(jo), BandImporter::acceptData, BandImporter::onFailure);
    }

    public static void loadHapi(URI uri) {
        Task.submit(uri.toString(), () -> BandReaderHapi.readUri(uri), data -> {
            if (data != null)
                acceptData(data);
        }, BandImporter::onFailure);
    }

    public static void loadCdf(URI uri) throws Exception {
        List<BandData> bands = BandReaderCdf.read(uri);
        if (bands.isEmpty())
            return;
        long[] dates = bands.getFirst().dates();
        if (dates.length == 0)
            return;

        EventQueue.invokeLater(() -> {
            bands.forEach(BandImporter::acceptData);
            DrawController.setSelectedInterval(dates[0], dates[dates.length - 1]);
        });
    }

    static void acceptData(BandData data) {
        TimelineLayers layers = Timelines.getLayers();
        Band band = layers.addBand(data.bandType());
        boolean hasDataChanged = band.addToCache(data.values(), data.dates());
        if (hasDataChanged)
            layers.updateRow(band);
    }

    private record BandLoad(JSONObject jo) implements Callable<BandData> {
        @Override
        public BandData call() throws Exception {
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
            return new BandData(bandType, dates, values);
        }
    }

    private static void onFailure(String ignoredLogContext, Throwable t) {
        Log.error(t);
    }

    private BandImporter() {}
}
