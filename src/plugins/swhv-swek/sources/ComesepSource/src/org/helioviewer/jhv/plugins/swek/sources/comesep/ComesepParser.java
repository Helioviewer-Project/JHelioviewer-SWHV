package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.util.Iterator;
import java.util.Locale;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParser;
import org.json.JSONException;
import org.json.JSONObject;

public class ComesepParser implements SWEKParser {

    @Override
    public JHVEvent parseEventJSON(JSONObject json, JHVEventType type, int id, long start, long end) throws JSONException {
        JHVEvent currentEvent = new JHVEvent(type, id, start, end);
        if (!parseResult(json, currentEvent)) {
            return null;
        }
        return currentEvent;
    }

    private boolean parseResult(JSONObject result, JHVEvent currentEvent) throws JSONException {
        Iterator<?> keys = result.keys();
        while (keys.hasNext()) {
            parseParameter(result, keys.next(), currentEvent);
        }
        return true;
    }

    private void parseParameter(JSONObject result, Object key, JHVEvent currentEvent) throws JSONException {
        if (key instanceof String) {
            String keyString = (String) key;
            String value = null;
            if (!result.isNull(keyString)) {
                value = result.optString(keyString);
            } else {
                return;
            }

            String lowerkey = keyString.toLowerCase(Locale.ENGLISH);
            if (!(lowerkey.equals("atearliest") || lowerkey.equals("atlatest") || lowerkey.equals("begin_time_value") || lowerkey.equals("end_time_value") || lowerkey.equalsIgnoreCase("liftoffduration_value"))) {
                value = value.trim();
                if (value.length() != 0)
                    currentEvent.addParameter(keyString, value);
            }
        }
    }

}
