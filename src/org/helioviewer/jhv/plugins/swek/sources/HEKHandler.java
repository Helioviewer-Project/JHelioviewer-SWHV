package org.helioviewer.jhv.plugins.swek.sources;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.database.DatabaseField;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.event.GOESLevel;
import org.helioviewer.jhv.event.JHVEvent;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKHandler;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unchecked")
public class HEKHandler extends SWEKHandler {

    private static final String BASE_URL = "https://www.lmsal.com/hek/her?";

    private enum HEKEventEnum {
        ACTIVE_REGION("ActiveRegion", "Active Region", "AR"),
        CORONAL_DIMMING("CoronalDimming", "Coronal Dimming", "CD"),
        CORONAL_HOLE("CoronalHole", "Coronal Hole", "CH"),
        CORONAL_MASS_EJECTION("CME", "Coronal Mass Ejection", "CE"),
        CORONAL_WAVE("CoronalWave", "Coronal Wave", "CW"),
        EMERGING_FLUX("EmergingFlux", "Emerging Flux", "EF"),
        ERUPTION("Eruption", "Eruption", "ER"),
        FILAMENT("Filament", "Filament", "FI"),
        FILAMENT_ERUPTION("FilamentEruption", "Filament Eruption", "FE"),
        FLARE("Flare", "Flare", "FL"),
        SUNSPOT("Sunspot", "Sunspot", "SS"),
        UNKNOWN("Unknown", "Unknown", "UK");

        // The abbreviation of the HEKEvent
        private final String eventAbbreviation;
        // The name of the SWEK Event
        private final String swekEventName;

        HEKEventEnum(String _hekEventName, String _swekEventName, String _eventAbbreviation) {
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
    protected boolean parseRemote(JSONObject eventJSON, SWEKSupplier supplier) {
        try {
            JSONArray results = eventJSON.getJSONArray("result");
            int len = results.length();
            List<EventDatabase.Event2Db> event2dbList = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                JSONObject result = results.getJSONObject(i);
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

                ArrayList<DatabaseField> paramList = new ArrayList<>();
                for (Map.Entry<String, String> fieldEntry : supplier.getGroup().getAllDatabaseFields().entrySet()) {
                    String dbType = fieldEntry.getValue();
                    String fieldName = fieldEntry.getKey();
                    String lfieldName = fieldName.toLowerCase();
                    if (!result.isNull(lfieldName)) {
                        switch (dbType) {
                            case DatabaseField.INTEGER ->
                                    paramList.add(new DatabaseField(fieldName, result.getInt(lfieldName)));
                            case DatabaseField.TEXT ->
                                    paramList.add(new DatabaseField(fieldName, result.getString(lfieldName)));
                            case DatabaseField.REAL ->
                                    paramList.add(new DatabaseField(fieldName, result.getDouble(lfieldName)));
                        }
                    }
                }
                try (ByteArrayOutputStream baos = JSONUtils.compressJSON(result)) {
                    event2dbList.add(new EventDatabase.Event2Db(baos.toByteArray(), start, end, archiv, uid, paramList));
                }
            }
            EventDatabase.dump_event2db(event2dbList, supplier);
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
        return true;
    }

    @Override
    protected boolean parseAssociations(JSONObject eventJSON) {
        JSONArray associations = eventJSON.getJSONArray("association");
        int len = associations.length();
        JHVEvent.LinkRef[] links = new JHVEvent.LinkRef[len];
        for (int i = 0; i < len; i++) {
            JSONObject asobj = associations.getJSONObject(i);
            links[i] = new JHVEvent.LinkRef(asobj.getString("first_ivorn"), asobj.getString("second_ivorn"));
        }
        return EventDatabase.dump_association2db(links) != -1;
    }

    @Override
    protected URI createURI(SWEKGroup group, long start, long end, List<SWEK.Param> params, int page) throws Exception {
        StringBuilder baseURL = new StringBuilder(BASE_URL + "cmd=search&type=column");
        baseURL.append("&event_type=").append(HEKEventEnum.getHEKEventAbbreviation(group.getName()));
        baseURL.append("&event_coordsys=helioprojective&x1=-3600&x2=3600&y1=-3600&y2=3600&cosec=2");
        baseURL.append("&param0=event_starttime&op0=").append(SWEK.Operand.SMALLER_OR_EQUAL.encodedRepresentation);
        baseURL.append("&value0=").append(TimeUtils.format(end));
        appendParams(baseURL, params);
        baseURL.append("&event_starttime=").append(TimeUtils.format(start));
        long max = Math.max(System.currentTimeMillis(), end);
        baseURL.append("&event_endtime=").append(TimeUtils.format(max));
        baseURL.append("&page=").append(page);
        return new URI(baseURL.toString());
    }

    private static void appendParams(StringBuilder baseURL, List<SWEK.Param> params) {
        int paramCount = 1;
        for (SWEK.Param p : params) {
            if ("provider".equalsIgnoreCase(p.name())) {
                String encodedValue = URLEncoder.encode(p.value(), StandardCharsets.UTF_8);
                baseURL.append("&param").append(paramCount).append('=').append("frm_name").
                        append("&op").append(paramCount).append('=').append(p.operand().encodedRepresentation).
                        append("&value").append(paramCount).append('=').append(encodedValue);
                paramCount++;
            }
        }
    }

    @Override
    public JHVEvent parseEventJSON(JSONObject json, SWEKSupplier supplier, int id, long start, long end, boolean full) throws JSONException {
        JHVEvent currentEvent = new JHVEvent(supplier, id, start, end);
        HEKParser.parseResult(json, currentEvent, full);
        currentEvent.finishParams();

        return currentEvent;
    }

}
