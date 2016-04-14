package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.util.Iterator;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParser;
import org.json.JSONException;
import org.json.JSONObject;

public class ComesepParser implements SWEKParser {

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
            String lowerkey = keyString.toLowerCase();
            String value = null;
            if (!result.isNull(keyString)) {
                value = result.optString(keyString);
            } else {
                return;
            }

            if (!(lowerkey.equals("atearliest") || lowerkey.equals("atlatest") || lowerkey.equals("begin_time_value") || lowerkey.equals("end_time_value") || lowerkey.equalsIgnoreCase("liftoffduration_value"))) {
                currentEvent.addParameter(keyString, value);
            }
        }
    }

    @Override
    public JHVEvent parseEventJSON(String json, JHVEventType type, int id, long start, long end) throws JSONException {
        JSONObject result = new JSONObject(json);
        String name = type.getEventType().getEventName();

        final JHVEvent currentEvent = new JHVEvent(name, name, type, id, start, end);
        boolean success = parseResult(result, currentEvent);
        if (!success) {
            return null;
        }

        return currentEvent;
    }
}
