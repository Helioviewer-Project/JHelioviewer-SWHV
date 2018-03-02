package org.helioviewer.jhv.plugins.eve.lines;

import org.helioviewer.jhv.timelines.band.BandType;
import org.json.JSONArray;
import org.json.JSONObject;

class EVEResponse {

    final String bandName;
    final BandType bandType;
    final long[] dates;
    final float[] values;

    EVEResponse(JSONObject jo) {
        bandName = jo.optString("timeline", "");
        bandType = jo.has("bandType") ? new BandType(jo.getJSONObject("bandType")) : null;

        double multiplier = jo.optDouble("multiplier", 1);
        JSONArray data = jo.getJSONArray("data");
        int length = data.length();
        values = new float[length];
        dates = new long[length];
        for (int i = 0; i < length; i++) {
            JSONArray entry = data.getJSONArray(i);
            dates[i] = entry.getLong(0) * 1000;
            values[i] = (float) (entry.getDouble(1) * multiplier);
        }
    }

}
