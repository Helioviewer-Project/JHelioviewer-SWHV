package org.helioviewer.jhv.plugins.swek.sources;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.event.JHVEvent;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKHandler;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.UriTemplate;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FHNWHandler extends SWEKHandler {

    private static final UriTemplate queryTemplate = new UriTemplate("https://tap.cs.technik.fhnw.ch/__system__/tap/run/tap/sync",
            UriTemplate.vars().set("REQUEST", "doQuery").set("LANG", "ADQL").set("FORMAT", "json"));

    @Override
    protected RemotePage parseRemotePage(JSONObject eventJSON, SWEKSupplier supplier) throws Exception {
        JSONArray params = eventJSON.getJSONArray("columns");
        int plen = params.length();
        JSONArray events = eventJSON.getJSONArray("data");
        int len = events.length();

        List<SWEK.RemoteEvent> event2dbList = new ArrayList<>(len);
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
                    event2dbList.add(new SWEK.RemoteEvent(baos.toByteArray(), start, end, archiv, uid, List.of()));
                }
            } else
                Log.warn("Inconsistent event parameter list length");
        }

        return new RemotePage(eventJSON.optBoolean("overmax", false), event2dbList, List.of());
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
    protected URI createURI(SWEKSupplier supplier, long start, long end, List<SWEK.Param> params, int page) throws Exception {
        String adql = "SELECT TOP 10 * FROM rhessi_flares.epn_core WHERE" +
                " start_time >= '" + "2002-01-01T00:00:00" + //TimeUtils.format(start) +
                "' AND end_time <= '" + TimeUtils.format(end) + "' ORDER BY start_time";
        return new URI(queryTemplate.expand(UriTemplate.vars().set("QUERY", adql)));
    }

}
