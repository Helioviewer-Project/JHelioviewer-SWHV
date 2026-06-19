package org.helioviewer.jhv.plugins.swek.sources;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.event.GOESLevel;
import org.helioviewer.jhv.event.JHVEvent;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKHandler;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HEKHandler extends SWEKHandler {

    private static final String BASE_URL = "https://www.lmsal.com/hek/her?";
    private static final String SMALLER_OR_EQUAL = URLEncoder.encode("<=", StandardCharsets.UTF_8);
    private static final String STRING_EQUALS = URLEncoder.encode("==", StandardCharsets.UTF_8);

    private enum HEKEventEnum {
        ACTIVE_REGION("Active Region", "AR"),
        CORONAL_DIMMING("Coronal Dimming", "CD"),
        CORONAL_HOLE("Coronal Hole", "CH"),
        CORONAL_MASS_EJECTION("Coronal Mass Ejection", "CE"),
        CORONAL_WAVE("Coronal Wave", "CW"),
        EMERGING_FLUX("Emerging Flux", "EF"),
        ERUPTION("Eruption", "ER"),
        FILAMENT("Filament", "FI"),
        FILAMENT_ERUPTION("Filament Eruption", "FE"),
        FLARE("Flare", "FL"),
        FLARE_TRIGGER("Flare Trigger", "FL"),
        SUNSPOT("Sunspot", "SS"),
        UNKNOWN("Unknown", "UK");

        // The abbreviation of the HEKEvent
        private final String eventAbbreviation;
        // The name of the SWEK Event
        private final String swekEventName;

        HEKEventEnum(String _swekEventName, String _eventAbbreviation) {
            eventAbbreviation = _eventAbbreviation;
            swekEventName = _swekEventName;
        }

        static String getHEKEventAbbreviation(String eventType) {
            for (HEKEventEnum event : values()) {
                if (event.swekEventName.equals(eventType)) {
                    return event.eventAbbreviation;
                }
            }
            return UNKNOWN.eventAbbreviation;
        }
    }

    @Override
    protected List<SWEK.RemoteEvent> parseEvents(JSONObject eventJSON, SWEKSupplier supplier) throws Exception {
        JSONArray results = eventJSON.getJSONArray("result");
        int len = results.length();
        List<SWEK.RemoteEvent> event2dbList = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            JSONObject result = results.getJSONObject(i);
            if (!isSupplierEvent(result, supplier))
                continue;

            if (result.has("fl_goescls"))
                result.put("fl_val", GOESLevel.getFloatValue(result.getString("fl_goescls")));

            long start = TimeUtils.parse(result.getString("event_starttime"));
            long end = TimeUtils.parse(result.getString("event_endtime"));
            if (end < start) {
                Log.warn("Event end before start: " + result);
                continue;
            }

            long archiv = TimeUtils.parse(result.getString("kb_archivdate"));
            String uid = result.getString("kb_archivid");

            ArrayList<SWEK.RemoteParameter> paramList = new ArrayList<>();
            for (Map.Entry<String, String> fieldEntry : supplier.group().getAllDatabaseFields().entrySet()) {
                String dbType = fieldEntry.getValue();
                String fieldName = fieldEntry.getKey();
                String lfieldName = fieldName.toLowerCase();
                if (!result.isNull(lfieldName)) {
                    switch (dbType) {
                        case "INTEGER" -> paramList.add(new SWEK.RemoteParameter(fieldName, result.getInt(lfieldName)));
                        case "TEXT" -> paramList.add(new SWEK.RemoteParameter(fieldName, result.getString(lfieldName)));
                        case "REAL" -> paramList.add(new SWEK.RemoteParameter(fieldName, result.getDouble(lfieldName)));
                    }
                }
            }
            try (ByteArrayOutputStream baos = JSONUtils.compressJSON(result)) {
                event2dbList.add(new SWEK.RemoteEvent(baos.toByteArray(), start, end, archiv, uid, paramList));
            }
        }
        return event2dbList;
    }

    @Override
    protected List<JHVEvent.LinkRef> parseAssociations(JSONObject eventJSON, SWEKSupplier supplier) {
        Set<String> acceptedUids = getSupplierUids(eventJSON, supplier);
        JSONArray associations = eventJSON.getJSONArray("association");
        int len = associations.length();
        List<JHVEvent.LinkRef> links = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            JSONObject asobj = associations.getJSONObject(i);
            String first = asobj.getString("first_ivorn");
            String second = asobj.getString("second_ivorn");
            if (acceptedUids.contains(first) || acceptedUids.contains(second))
                links.add(new JHVEvent.LinkRef(first, second));
        }
        return links;
    }

    private static Set<String> getSupplierUids(JSONObject eventJSON, SWEKSupplier supplier) {
        JSONArray results = eventJSON.getJSONArray("result");
        int len = results.length();
        Set<String> uids = new HashSet<>();
        for (int i = 0; i < len; i++) {
            JSONObject result = results.getJSONObject(i);
            if (isSupplierEvent(result, supplier))
                uids.add(result.getString("kb_archivid"));
        }
        return uids;
    }

    private static boolean isSupplierEvent(JSONObject result, SWEKSupplier supplier) {
        return supplier.supplierName().equals(result.optString("frm_name"));
    }

    @Override
    protected URI createURI(SWEKSupplier supplier, long start, long end, List<SWEK.Param> params, int page) throws Exception {
        StringBuilder baseURL = new StringBuilder(BASE_URL + "cmd=search&type=column");
        baseURL.append("&event_type=").append(HEKEventEnum.getHEKEventAbbreviation(supplier.group().getName()));
        baseURL.append("&event_coordsys=helioprojective&x1=-3600&x2=3600&y1=-3600&y2=3600&cosec=2");
        baseURL.append("&param0=event_starttime&op0=").append(SMALLER_OR_EQUAL).append("&value0=").append(TimeUtils.format(end));
        appendSupplierFilter(baseURL, supplier);
        baseURL.append("&event_starttime=").append(TimeUtils.format(start));
        long max = Math.max(System.currentTimeMillis(), end);
        baseURL.append("&event_endtime=").append(TimeUtils.format(max));
        baseURL.append("&page=").append(page);
        return new URI(baseURL.toString());
    }

    private static void appendSupplierFilter(StringBuilder baseURL, SWEKSupplier supplier) {
        String encodedValue = URLEncoder.encode(supplier.supplierName(), StandardCharsets.UTF_8);
        baseURL.append("&param1=frm_name").append("&op1=").append(STRING_EQUALS).append("&value1=").append(encodedValue);
    }

    @Override
    public JHVEvent parseEventJSON(JSONObject json, SWEKSupplier supplier, int id, long start, long end, boolean full) throws JSONException {
        JHVEvent currentEvent = new JHVEvent(supplier, id, start, end);
        HEKParser.parseResult(json, currentEvent, full);
        currentEvent.finishParams();
        return currentEvent;
    }
}
