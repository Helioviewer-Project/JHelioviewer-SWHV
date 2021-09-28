package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.conversion.GOESLevel;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.database.JHVDatabaseParam;
import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.events.SWEKHandler;
import org.helioviewer.jhv.events.SWEKOperand;
import org.helioviewer.jhv.events.SWEKParam;
import org.helioviewer.jhv.events.SWEKSupplier;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unchecked")
public class HEKHandler extends SWEKHandler {

    private static final String _baseURL = "https://www.lmsal.com/hek/her?";

    @Override
    protected boolean parseRemote(JSONObject eventJSON, SWEKSupplier supplier) {
        try {
            JSONArray results = eventJSON.getJSONArray("result");
            int len = results.length();
            ArrayList<EventDatabase.Event2Db> event2db_list = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                JSONObject result = results.getJSONObject(i);
                if (result.has("fl_goescls"))
                    result.put("fl_val", GOESLevel.getFloatValue(result.getString("fl_goescls")));

                long start = TimeUtils.parse(result.getString("event_starttime"));
                long end = TimeUtils.parse(result.getString("event_endtime"));
                if (end < start) {
                    Log.error("Event end before start: " + result);
                    continue;
                }

                long archiv = TimeUtils.parse(result.getString("kb_archivdate"));
                String uid = result.getString("kb_archivid");

                ArrayList<JHVDatabaseParam> paramList = new ArrayList<>();
                for (Map.Entry<String, String> entry : supplier.getGroup().getAllDatabaseFields().entrySet()) {
                    String dbType = entry.getValue();
                    String fieldName = entry.getKey();
                    String lfieldName = fieldName.toLowerCase(Locale.ENGLISH);
                    if (!result.isNull(lfieldName)) {
                        switch (dbType) {
                            case JHVDatabaseParam.DBINTTYPE -> paramList.add(new JHVDatabaseParam(result.getInt(lfieldName), fieldName));
                            case JHVDatabaseParam.DBSTRINGTYPE -> paramList.add(new JHVDatabaseParam(result.getString(lfieldName), fieldName));
                            case JHVDatabaseParam.DBDOUBLETYPE -> paramList.add(new JHVDatabaseParam(result.getDouble(lfieldName), fieldName));
                        }
                    }
                }
                try (ByteArrayOutputStream baos = JSONUtils.compressJSON(result)) {
                    event2db_list.add(new EventDatabase.Event2Db(baos.toByteArray(), start, end, archiv, uid, paramList));
                }
            }
            EventDatabase.dump_event2db(event2db_list, supplier);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean parseAssociations(JSONObject eventJSON) {
        JSONArray associations = eventJSON.getJSONArray("association");
        int len = associations.length();
        Pair<?, ?>[] assocs = new Pair<?, ?>[len];
        for (int i = 0; i < len; i++) {
            JSONObject asobj = associations.getJSONObject(i);
            assocs[i] = new Pair<>(asobj.getString("first_ivorn"), asobj.getString("second_ivorn"));
        }
        return EventDatabase.dump_association2db((Pair<String, String>[]) assocs) != -1;
    }

    @Override
    protected String createURL(SWEKGroup group, long start, long end, List<SWEKParam> params, int page) {
        StringBuilder baseURL = new StringBuilder(_baseURL);
        baseURL.append("cmd=search&type=column&");
        baseURL.append("event_type=").append(HEKEventEnum.getHEKEventAbbreviation(group.getName())).append('&');
        baseURL.append("event_coordsys=helioprojective&x1=-3600&x2=3600&y1=-3600&y2=3600&cosec=2&");
        baseURL.append("param0=event_starttime&op0=").append(SWEKOperand.SMALLER_OR_EQUAL.encodedRepresentation).append('&');
        baseURL.append("value0=").append(TimeUtils.format(end)).append('&');
        appendParams(baseURL, params);
        baseURL.append("event_starttime=").append(TimeUtils.format(start)).append('&');
        long max = Math.max(System.currentTimeMillis(), end);
        baseURL.append("event_endtime=").append(TimeUtils.format(max)).append('&');
        baseURL.append("page=").append(page);
        return baseURL.toString();
    }

    private static void appendParams(StringBuilder baseURL, List<SWEKParam> params) {
        int paramCount = 1;

        for (SWEKParam param : params) {
            if (param.param.equalsIgnoreCase("provider")) {
                String encodedValue = URLEncoder.encode(param.value, StandardCharsets.UTF_8);
                baseURL.append("param").append(paramCount).append('=').append("frm_name").append('&').append("op").append(paramCount).append('=').append(param.operand.encodedRepresentation).append('&').append("value").append(paramCount).append('=').append(encodedValue).append('&');
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
