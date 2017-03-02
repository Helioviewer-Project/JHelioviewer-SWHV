package org.helioviewer.jhv.plugins.eveplugin.lines;

import java.io.IOException;

import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.timelines.data.Band;
import org.helioviewer.jhv.timelines.data.BandType;
import org.helioviewer.jhv.timelines.data.DownloadController;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class DownloadThread extends JHVWorker<Pair<float[], long[]>, Void> {

    private final Interval interval;
    private final Band band;

    public DownloadThread(Band _band, Interval _interval) {
        interval = _interval;
        band = _band;
    }

    public Interval getInterval() {
        return interval;
    }

    public Band getBand() {
        return band;
    }

    @Override
    protected Pair<float[], long[]> backgroundWork() {
        try {
            JSONObject json = JSONUtils.getJSONStream(new DownloadStream(buildRequest(interval, band.getBandType())).getInput());

            double multiplier = 1.0;
            if (json.has("multiplier")) {
                multiplier = json.getDouble("multiplier");
            }

            JSONArray data = json.getJSONArray("data");
            int length = data.length();
            if (length == 0) {
                return null;
            }

            float[] values = new float[length];
            long[] dates = new long[length];
            for (int i = 0; i < length; i++) {
                JSONArray entry = data.getJSONArray(i);
                dates[i] = entry.getLong(0) * 1000;
                values[i] = (float) (entry.getDouble(1) * multiplier);
            }

            return new Pair<>(values, dates);
        } catch (JSONException | IOException e) {
            Log.error("Error Parsing the EVE Response ", e);
        }
        return null;
    }

    @Override
    protected void done() {
        if (!isCancelled()) {
            try {
                Pair<float[], long[]> p = get();
                if (p != null)
                    band.addToCache(p.a, p.b);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        DownloadController.downloadFinished(band, interval);
    }

    private static String buildRequest(Interval interval, BandType type) {
        String urlf = type.getBaseURL() + "start_date=%s&end_date=%s&timeline=%s&data_format=json";
        return String.format(urlf, TimeUtils.dateFormat.format(interval.start), TimeUtils.dateFormat.format(interval.end), type.getName());
    }

}
