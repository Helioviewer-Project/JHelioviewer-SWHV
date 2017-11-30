package org.helioviewer.jhv.plugins.eve.lines;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.io.NetStream;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.timelines.band.BandType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class EVEResponse {

    final String bandName;
    final BandType bandType;
    final long[] dates;
    final float[] values;

    private EVEResponse(String _bandName, BandType _bandType, long[] _dates, float[] _values) {
        bandName = _bandName;
        bandType = _bandType;
        dates = _dates;
        values = _values;
    }

    static EVEResponse get(URI uri) {
        try (NetStream ns = new NetStream(uri.toURL())) {
            JSONObject jo = JSONUtils.getJSONStream(ns.getInput());

            String bandName = jo.optString("timeline", "");
            double multiplier = jo.optDouble("multiplier", 1);
            BandType bandType = jo.has("bandType") ? new BandType(jo.getJSONObject("bandType")) : null;

            JSONArray data = jo.getJSONArray("data");
            int length = data.length();
            float[] values = new float[length];
            long[] dates = new long[length];
            for (int i = 0; i < length; i++) {
                JSONArray entry = data.getJSONArray(i);
                dates[i] = entry.getLong(0) * 1000;
                values[i] = (float) (entry.getDouble(1) * multiplier);
            }

            return new EVEResponse(bandName, bandType, dates, values);
        } catch (JSONException | IOException e) {
            Log.error("Error parsing the EVE Response ", e);
        }
        return null;
    }

}
