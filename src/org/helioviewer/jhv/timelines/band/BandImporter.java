package org.helioviewer.jhv.timelines.band;

import java.awt.EventQueue;
import java.net.URI;
import java.util.ArrayList;
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
        Task.submit(uri.toString(), () -> BandReaderHapi.readUri(uri), BandImporter::acceptData,
                BandImporter::onFailure);
    }

    public static void loadCdf(URI uri) throws Exception {
        List<BandData> bands = BandReaderCdf.read(uri);
        if (bands.isEmpty())
            return;
        long[] dates = bands.getFirst().dates();
        if (dates.length == 0)
            return;

        EventQueue.invokeLater(() -> {
            acceptData(bands);
            DrawController.setSelectedInterval(dates[0], dates[dates.length - 1]);
        });
    }

    private static void acceptData(List<BandData> data) {
        if (data.isEmpty())
            return;

        TimelineLayers layers = Timelines.getLayers();
        List<Band> bands = layers.addBands(data.stream().map(BandData::bandType).toList());
        List<Band> changedBands = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            BandData bandData = data.get(i);
            Band band = bands.get(i);
            if (band.addToCache(bandData.values(), bandData.dates()))
                changedBands.add(band);
        }
        layers.updateRows(changedBands);
    }

    private record BandLoad(JSONObject jo) implements Callable<List<BandData>> {
        @Override
        public List<BandData> call() throws Exception {
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
            return List.of(new BandData(bandType, dates, values));
        }
    }

    private static void onFailure(String logContext, Throwable t) {
        Log.error(logContext, t);
    }

    private BandImporter() {}
}
