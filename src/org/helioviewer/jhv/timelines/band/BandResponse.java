package org.helioviewer.jhv.timelines.band;

import org.json.JSONArray;
import org.json.JSONObject;

class BandResponse {

    final String bandName;
    final BandType bandType;
    final long[] dates;
    final float[] values;

    BandResponse(JSONObject jo) {
        JSONObject bo = jo.optJSONObject("bandType");
        bandType = bo == null ? null : new BandType(bo);
        bandName = jo.optString("timeline", "Unknown");

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
