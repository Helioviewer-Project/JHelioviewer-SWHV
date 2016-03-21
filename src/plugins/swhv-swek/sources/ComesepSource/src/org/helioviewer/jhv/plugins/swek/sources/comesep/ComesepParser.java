package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.awt.EventQueue;
import java.util.Date;
import java.util.Iterator;

import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParameter;
import org.helioviewer.jhv.data.datatype.event.SWEKParser;
import org.helioviewer.jhv.data.datatype.event.SWEKSource;
import org.json.JSONException;
import org.json.JSONObject;

public class ComesepParser implements SWEKParser {

    private SWEKEventType eventType;
    private SWEKSource eventSource;

    private boolean parseResult(JSONObject result, ComesepEvent currentEvent) throws JSONException {
        Iterator<?> keys = result.keys();
        while (keys.hasNext()) {
            parseParameter(result, keys.next(), currentEvent);
        }
        return true;
    }

    private void parseParameter(JSONObject result, Object key, ComesepEvent currentEvent) throws JSONException {
        if (key instanceof String) {
            String keyString = (String) key;
            String lowerkey = keyString.toLowerCase();
            String value = null;
            if (!result.isNull(keyString))
                value = result.optString(keyString);
            else
                return;

            if (lowerkey.equals("atearliest") || lowerkey.equals("atlatest") || lowerkey.equals("begin_time_value") || lowerkey.equals("end_time_value") || lowerkey.equalsIgnoreCase("liftoffduration_value")) {
            }
            else {
                boolean visible = false;
                boolean configured = false;
                String displayName = keyString;
                if (eventType.containsParameter(keyString) || eventSource.containsParameter(keyString)) {
                    configured = true;
                    SWEKParameter p = currentEvent.getJHVEventType().getEventType().getParameter(keyString);
                    if (p == null) {
                        p = currentEvent.getJHVEventType().getSupplier().getSource().getParameter(keyString);
                    }
                    if (p != null) {
                        configured = true;
                        visible = p.isDefaultVisible();
                        displayName = p.getParameterDisplayName();
                    }
                    else {
                        displayName = keyString.replaceAll("_", " ").trim();
                    }
                }
                JHVEventParameter parameter = new JHVEventParameter(keyString, displayName, value);
                currentEvent.addParameter(parameter, visible, configured);
            }
        }
    }

    @Override
    public boolean parseEventJSON(String json, JHVEventType type, int id, long start, long end) throws JSONException {
        JSONObject result = new JSONObject(json);
        String name = type.getEventType().getEventName();

        ComesepEvent currentEvent = new ComesepEvent(name, name, type, id, new Date(start), new Date(end));
        boolean success = parseResult(result, currentEvent);
        if (!success) {
            return false;
        }
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JHVEventCache.getSingletonInstance().add(currentEvent);
            }
        });
        return true;
    }
}
