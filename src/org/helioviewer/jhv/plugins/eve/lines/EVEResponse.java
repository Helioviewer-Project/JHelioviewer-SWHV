package org.helioviewer.jhv.plugins.eve.lines;

import java.io.IOException;
import java.net.URI;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.io.DownloadStream;
import org.helioviewer.jhv.log.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class EVEResponse {

    final String bandType;
    final long[] dates;
    final float[] values;

    EVEResponse(String _bandType, long[] _dates, float[] _values) {
        bandType = _bandType;
        dates = _dates;
        values = _values;
    }

    static EVEResponse get(URI uri) {
        try {
            JSONObject json = JSONUtils.getJSONStream(new DownloadStream(uri.toURL()).getInput());

            String bandType = json.optString("timeline", "");
            JSONArray data = json.getJSONArray("data");
            int length = data.length();
            if (length == 0)
                return null;

            double multiplier = json.optDouble("multiplier", 1);
            float[] values = new float[length];
            long[] dates = new long[length];
            for (int i = 0; i < length; i++) {
                JSONArray entry = data.getJSONArray(i);
                dates[i] = entry.getLong(0) * 1000;
                values[i] = (float) (entry.getDouble(1) * multiplier);
            }

            return new EVEResponse(bandType, dates, values);
        } catch (JSONException | IOException e) {
            Log.error("Error parsing the EVE Response ", e);
        }
        return null;
    }

}
