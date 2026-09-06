package org.helioviewer.jhv.plugins.swek.sources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.event.GOESLevel;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKHandler;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HEKHandler extends SWEKHandler {

    private static final String BASE_URL = "https://www.lmsal.com/hek/her?";

    @Override
    protected RemotePage parseRemotePage(JSONObject eventJSON, SWEKSupplier supplier) throws Exception {
        // Missing pagination metadata must not turn a partial response into cached coverage.
        boolean overmax = eventJSON.getBoolean("overmax");
        JSONArray results = eventJSON.getJSONArray("result");
        int len = results.length();
        List<SWEKHandler.RemoteEvent> event2dbList = new ArrayList<>(len);
        Set<String> acceptedUids = new HashSet<>();
        for (int i = 0; i < len; i++) {
            try {
                JSONObject result = results.getJSONObject(i);
                if (!isSupplierEvent(result, supplier))
                    continue;

                SWEKHandler.RemoteEvent event = parseRemoteEvent(result, supplier);
                event2dbList.add(event);
                acceptedUids.add(event.uid());
            } catch (JSONException | DateTimeException e) {
                throw new IOException("Malformed HEK event at result index " + i, e);
            }
        }
        return new RemotePage(overmax, event2dbList, parseAssociations(eventJSON, acceptedUids));
    }

    private static SWEKHandler.RemoteEvent parseRemoteEvent(JSONObject result, SWEKSupplier supplier) throws IOException {
        addGoesValue(result);

        long start = TimeUtils.parse(result.getString("event_starttime"));
        long end = TimeUtils.parse(result.getString("event_endtime"));
        if (end < start) {
            throw new IOException("HEK event end before start: " + result.optString("kb_archivid"));
        }

        String archiveDate = result.optString("kb_archivdate");
        long archiv = archiveDate.isBlank() ? start : TimeUtils.parse(archiveDate);
        String uid = result.getString("kb_archivid");
        if (uid.isBlank())
            throw new IOException("HEK event has an empty archive ID");

        HashMap<String, Number> indexedValues = new HashMap<>();
        for (Map.Entry<String, SWEK.NumericType> field : SWEKCatalog.indexedParameters(supplier).entrySet()) {
            String fieldName = field.getKey();
            String lfieldName = fieldName.toLowerCase();
            if (result.isNull(lfieldName))
                continue;

            try {
                switch (field.getValue()) {
                    case INTEGER -> indexedValues.put(fieldName, result.getInt(lfieldName));
                    case DECIMAL -> indexedValues.put(fieldName, result.getDouble(lfieldName));
                }
            } catch (JSONException e) {
                Log.warn("Ignoring malformed HEK field " + fieldName + " in " + uid, e);
            }
        }
        try (ByteArrayOutputStream baos = JSONUtils.compressJSON(result)) {
            return new SWEKHandler.RemoteEvent(baos.toByteArray(), start, end, archiv, uid, indexedValues);
        }
    }

    private static List<SolarEvent.LinkRef> parseAssociations(JSONObject eventJSON, Set<String> acceptedUids) {
        JSONArray associations = eventJSON.optJSONArray("association");
        if (associations == null)
            return List.of();

        int len = associations.length();
        List<SolarEvent.LinkRef> links = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            try {
                JSONObject asobj = associations.getJSONObject(i);
                String first = asobj.getString("first_ivorn");
                String second = asobj.getString("second_ivorn");
                if (acceptedUids.contains(first) || acceptedUids.contains(second))
                    links.add(new SolarEvent.LinkRef(first, second));
            } catch (JSONException e) {
                Log.warn("Skipping malformed HEK association at index " + i, e);
            }
        }
        return links;
    }

    private static boolean isSupplierEvent(JSONObject result, SWEKSupplier supplier) {
        return supplier.supplierName().equals(result.getString("frm_name"));
    }

    private static void addGoesValue(JSONObject result) {
        String goesClass = result.optString("fl_goescls").trim();
        if (goesClass.isEmpty())
            return;
        try {
            result.put("jhv_goesflux", GOESLevel.getFloatValue(goesClass));
        } catch (IllegalArgumentException ignore) {}
    }

    @Override
    protected URI createURI(SWEKSupplier supplier, long start, long end, int page) throws Exception {
        StringBuilder baseURL = new StringBuilder(BASE_URL + "cmd=search&type=column");
        baseURL.append("&event_type=").append(getEventAbbreviation(supplier.group().getName()));
        baseURL.append("&event_coordsys=helioprojective&x1=-3600&x2=3600&y1=-3600&y2=3600&cosec=2");
        // HEK's supplier predicate excludes CACTus records. Filter the returned records locally instead.
        // HEK's default temporal search includes events overlapping either endpoint.
        baseURL.append("&event_starttime=").append(TimeUtils.format(start));
        baseURL.append("&event_endtime=").append(TimeUtils.format(end));
        // The downloader uses zero-based offsets, whereas HEK numbers pages from one.
        baseURL.append("&page=").append(page + 1);
        return new URI(baseURL.toString());
    }

    private static String getEventAbbreviation(String eventType) {
        return switch (eventType) {
            case "Active Region" -> "AR";
            case "Coronal Dimming" -> "CD";
            case "Coronal Hole" -> "CH";
            case "Coronal Mass Ejection" -> "CE";
            case "Coronal Wave" -> "CW";
            case "Emerging Flux" -> "EF";
            case "Eruption" -> "ER";
            case "Filament" -> "FI";
            case "Filament Eruption" -> "FE";
            case "Flare" -> "FL";
            case "Sunspot" -> "SS";
            default -> "UK";
        };
    }

    @Override
    public SolarEvent parseEventJSON(JSONObject json, SWEKSupplier supplier, int id, long start, long end, boolean full) throws JSONException {
        return new SolarEvent(supplier, id, start, end, new HEKGeometry(json).position(start, supplier.isCactus()),
                readCMEParameters(json, supplier), HEKParser.parseResult(json, supplier, full));
    }

    private static SolarEvent.CMEParameters readCMEParameters(JSONObject json, SWEKSupplier supplier) {
        SolarEvent.CMEParameters defaults = SolarEvent.CMEParameters.DEFAULT;
        if (!supplier.isCactus())
            return defaults;
        return new SolarEvent.CMEParameters(readDouble(json, "cme_radiallinvel", defaults.speedKmPerSecond()),
                readDouble(json, "event_coord1", defaults.principalAngleDegree()), readDouble(json, "cme_angularwidth", defaults.angularWidthDegree()));
    }

    private static double readDouble(JSONObject result, String parameter, double fallback) {
        try {
            return Double.parseDouble(result.optString(parameter).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
