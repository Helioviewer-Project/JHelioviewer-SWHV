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
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ComesepHandler extends SWEKHandler {

    private static final String BASE_URL = "http://swhv.oma.be/comesep/comeseprequestapi/getComesep.php?";

    @Override
    protected List<SWEK.RemoteEvent> parseEvents(JSONObject eventJSON, SWEKSupplier supplier) throws Exception {
        JSONArray results = eventJSON.getJSONArray("results");
        int len = results.length();
        List<SWEK.RemoteEvent> event2dbList = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            JSONObject result = results.getJSONObject(i);

            long start = result.getLong("atearliest") * 1000;
            long end = result.getLong("atlatest") * 1000;
            if (end < start) {
                Log.warn("Event end before start: " + result);
                continue;
            }

            if (result.has("liftoffduration_value")) {
                long cactusLiftOff = result.getLong("liftoffduration_value");
                end += cactusLiftOff * 60000;
            }

            long archiv = start;
            String uid = result.getString("alertid");
            try (ByteArrayOutputStream baos = JSONUtils.compressJSON(result)) {
                event2dbList.add(new SWEK.RemoteEvent(baos.toByteArray(), start, end, archiv, uid, List.of()));
            }
        }
        return event2dbList;
    }

    @Override
    protected List<JHVEvent.LinkRef> parseAssociations(JSONObject eventJSON) {
        JSONArray associations = eventJSON.getJSONArray("associations");
        int len = associations.length();
        List<JHVEvent.LinkRef> links = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            JSONObject asobj = associations.getJSONObject(i);
            links.add(new JHVEvent.LinkRef(asobj.getString("parent"), asobj.getString("child")));
        }
        return links;
    }

    @Override
    protected URI createURI(SWEKSupplier supplier, long start, long end, List<SWEK.Param> params, int page) throws Exception {
        return new URI(BASE_URL + "model=" + supplier.getSupplierName() + "&startdate=" + TimeUtils.format(start) + "&enddate=" + TimeUtils.format(end));
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
            if (result.isNull(key))
                continue;

            String lowKey = key.toLowerCase();
            if (!(lowKey.equals("atearliest") || lowKey.equals("atlatest") ||
                    lowKey.equals("begin_time_value") || lowKey.equals("end_time_value") ||
                    lowKey.startsWith("liftoff"))) {
                String value = result.optString(key).trim();
                if (!value.isEmpty()) {
                    if (lowKey.equals("atstrongest")) {
                        try {
                            value = TimeUtils.format(Long.parseLong(value) * 1000L);
                        } catch (Exception ignore) {}
                    }
                    currentEvent.addParameter(lowKey, value, true);
                }
            }
        }
    }
}
