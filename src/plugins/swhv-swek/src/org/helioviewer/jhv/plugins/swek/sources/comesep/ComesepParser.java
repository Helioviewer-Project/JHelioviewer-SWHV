package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.util.Iterator;
import java.util.Locale;

import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.event.JHVEvent;
import org.helioviewer.jhv.data.event.JHVEventType;
import org.helioviewer.jhv.data.event.SWEKParser;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

public class ComesepParser implements SWEKParser {

    @NotNull
    @Override
    public JHVEvent parseEventJSON(@NotNull JSONObject json, JHVEventType type, int id, long start, long end, boolean full) throws JSONException {
        JHVEvent currentEvent = new JHVEvent(type, id, start, end);

        currentEvent.initParams();
        parseResult(json, currentEvent);
        currentEvent.finishParams();

        return currentEvent;
    }

    private static void parseResult(@NotNull JSONObject result, @NotNull JHVEvent currentEvent) throws JSONException {
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
                            value = TimeUtils.apiDateFormat.format(Long.parseLong(value) * 1000L);
                        } catch (Exception ignore) {
                        }
                    }
                    currentEvent.addParameter(lowKey, value, true);
                }
            }
        }
    }

}
