package org.helioviewer.jhv.plugins.swek.sources;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.SWEK;
import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.events.SWEKHandler;
import org.helioviewer.jhv.events.SWEKSupplier;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FHNWHandler extends SWEKHandler {

    private static final String QUERY_URL = "https://tap.cs.technik.fhnw.ch/__system__/tap/run/tap";

    @Override
    protected boolean parseRemote(JSONObject eventJSON, SWEKSupplier supplier) {
        try {
            JSONArray params = eventJSON.getJSONArray("columns");
            int plen = params.length();
            JSONArray events = eventJSON.getJSONArray("data");
            int len = events.length();

            List<EventDatabase.Event2Db> event2dbList = new ArrayList<>(len);
            for (int j = 0; j < len; j++) {
                JSONArray event = events.getJSONArray(j);
                int elen = event.length();
                if (elen == plen) {
                    JSONObject result = new JSONObject();
                    for (int i = 0; i < elen; i++) {
                        if (!event.isNull(i))
                            result.put(params.getJSONObject(i).getString("name"), event.get(i));
                    }

                    long start = TimeUtils.parse(result.getString("start_time"));
                    long end = TimeUtils.parse(result.getString("end_time"));
                    if (start > end) {
                        Log.warn("Event end before start: " + result);
                        continue;
                    }

                    long archiv = start;
                    String uid = result.getString("granule_uid");
                    try (ByteArrayOutputStream baos = JSONUtils.compressJSON(result)) {
                        event2dbList.add(new EventDatabase.Event2Db(baos.toByteArray(), start, end, archiv, uid, new ArrayList<>()));
                    }
                } else
                    Log.warn("Inconsistent event parameter list length");
            }

            EventDatabase.dump_event2db(event2dbList, supplier);
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
        return true;
    }

    @Override
    public JHVEvent parseEventJSON(JSONObject json, SWEKSupplier supplier, int id, long start, long end, boolean full) throws JSONException {
        JHVEvent currentEvent = new JHVEvent(supplier, id, start, end);
        parseResult(json, currentEvent);
        currentEvent.finishParams();

        return currentEvent;
    }

    private static void parseResult(JSONObject result, JHVEvent currentEvent) throws JSONException {
        Iterator<String> keys = result.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!(key.equals("start_time") || key.equals("end_time"))) { // don't repeat
                String value = result.optString(key).trim();
                if (!value.isEmpty()) {
                    currentEvent.addParameter(key, value, true);
                }
            }
        }
    }

    @Override
    protected URI createURI(SWEKGroup group, long start, long end, List<SWEK.Param> params, int page) throws Exception {
        String adql = "SELECT TOP 10 * FROM rhessi_flares.epn_core WHERE" +
                " start_time >= '" + "2002-01-01T00:00:00" + //TimeUtils.format(start) +
                "' AND end_time <= '" + TimeUtils.format(end) + "' ORDER BY start_time";
        return new URI(QUERY_URL + "/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=json&QUERY=" + URLEncoder.encode(adql, StandardCharsets.UTF_8));
    }

    @Override
    protected boolean parseAssociations(JSONObject eventJSON) {
        return true;
    }

}
