package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.util.Iterator;
import java.util.Locale;

import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONException;
import org.json.JSONObject;

class ComesepParser {

    static void parseResult(JSONObject result, JHVEvent currentEvent) throws JSONException {
        Iterator<String> keys = result.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (result.isNull(key))
                return;

            String lowKey = key.toLowerCase(Locale.ENGLISH);
            if (!(lowKey.equals("atearliest") || lowKey.equals("atlatest") ||
                    lowKey.equals("begin_time_value") || lowKey.equals("end_time_value") ||
                    lowKey.startsWith("liftoff"))) {
                String value = result.optString(key).trim();
                if (!value.isEmpty()) {
                    if (lowKey.equals("atstrongest")) {
                        try {
                            value = TimeUtils.format(Long.parseLong(value) * 1000L);
                        } catch (Exception ignore) {
                        }
                    }
                    currentEvent.addParameter(lowKey, value, true);
                }
            }
        }
    }

}
