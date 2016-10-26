package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.util.Iterator;
import java.util.Locale;

import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParser;
import org.json.JSONException;
import org.json.JSONObject;

public class ComesepParser implements SWEKParser {

    @Override
    public JHVEvent parseEventJSON(JSONObject json, JHVEventType type, int id, long start, long end, boolean full) throws JSONException {
        JHVEvent currentEvent = new JHVEvent(type, id, start, end);

        currentEvent.initParams();
        parseResult(json, currentEvent);
        currentEvent.finishParams();

        return currentEvent;
    }

    private void parseResult(JSONObject result, JHVEvent currentEvent) throws JSONException {
        Iterator<?> keys = result.keys();
        while (keys.hasNext()) {
            parseParameter(result, keys.next(), currentEvent);
        }
    }

    private void parseParameter(JSONObject result, Object key, JHVEvent currentEvent) throws JSONException {
        if (key instanceof String) {
            String keyString = ((String) key).intern();
            String value;
            if (!result.isNull(keyString)) {
                value = result.optString(keyString);
            } else {
                return;
            }

            String lowerkey = keyString.toLowerCase(Locale.ENGLISH);
            if (!(lowerkey.equals("atearliest") || lowerkey.equals("atlatest") ||
                  lowerkey.equals("begin_time_value") || lowerkey.equals("end_time_value") ||
                  lowerkey.startsWith("liftoff"))) {
                value = value.trim();
                if (value.length() != 0) {
                    if (lowerkey.equals("atstrongest")) {
                        try {
                            value = TimeUtils.apiDateFormat.format(Long.parseLong(value) * 1000L);
                        } catch (Exception ignore) {
                        }
                    }
                    currentEvent.addParameter(lowerkey, value, true);
                }
            }
        }
    }

}
