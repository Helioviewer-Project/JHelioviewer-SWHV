package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.conversion.GOESLevel;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.event.JHVEventType;
import org.helioviewer.jhv.data.event.SWEKDownloader;
import org.helioviewer.jhv.data.event.SWEKEventType;
import org.helioviewer.jhv.data.event.SWEKParam;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.database.JHVDatabaseParam;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("unchecked")
public class HEKDownloader extends SWEKDownloader {

    private static final String _baseURL = "http://www.lmsal.com/hek/her?";

    private static void patch_event(JSONObject result, JHVEventType type) {
        boolean c1 = type.getEventType().getDisplayName().equals("Flare");
        boolean c2 = type.getSupplier().getSupplierName().equals("SWPC");
        boolean c = c1 && c2;
        if (c && result.has("fl_goescls")) {
            result.put("fl_val", GOESLevel.getFloatValue(result.getString("fl_goescls")));
        }
    }

    @Override
    protected boolean parseEvents(JSONObject eventJSON, JHVEventType type) {
        JSONArray results = eventJSON.getJSONArray("result");
        ArrayList<EventDatabase.Event2Db> event2db_list = new ArrayList<>();

        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);
            byte[] compressed;
            try {
                compressed = JSONUtils.compressJSON(result);
            } catch (IOException e) {
                Log.error("compression error");
                return false;
            }
            patch_event(result, type);
            String uid;
            long start;
            long end;
            long archiv;
            ArrayList<JHVDatabaseParam> paramList = new ArrayList<>();
            try {
                start = TimeUtils.parse(result.getString("event_starttime"));
                end = TimeUtils.parse(result.getString("event_endtime"));
                archiv = TimeUtils.parse(result.getString("kb_archivdate"));
                uid = result.getString("kb_archivid");

                HashMap<String, String> dbFields = type.getEventType().getAllDatabaseFields();
                for (Map.Entry<String, String> entry : dbFields.entrySet()) {
                    String dbType = entry.getValue();
                    String fieldName = entry.getKey();
                    String lfieldName = fieldName.toLowerCase(Locale.ENGLISH);
                    if (!result.isNull(lfieldName)) {
                        switch (dbType) {
                            case JHVDatabaseParam.DBINTTYPE:
                                paramList.add(new JHVDatabaseParam(JHVDatabaseParam.DBINTTYPE, result.getInt(lfieldName), fieldName));
                                break;
                            case JHVDatabaseParam.DBSTRINGTYPE:
                                paramList.add(new JHVDatabaseParam(JHVDatabaseParam.DBSTRINGTYPE, result.getString(lfieldName), fieldName));
                                break;
                            case JHVDatabaseParam.DBDOUBLETYPE:
                                paramList.add(new JHVDatabaseParam(JHVDatabaseParam.DBDOUBLETYPE, result.getDouble(lfieldName), fieldName));
                                break;
                            default:
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                return false;
            }

            event2db_list.add(new EventDatabase.Event2Db(compressed, start, end, archiv, uid, paramList));
        }

        EventDatabase.dump_event2db(event2db_list, type);

        return true;
    }

    @Override
    protected boolean parseAssociations(JSONObject eventJSON) {
        JSONArray associations = eventJSON.getJSONArray("association");
        int len = associations.length();
        Pair<?,?>[] assocs = new Pair<?,?>[len];
        for (int i = 0; i < len; i++) {
            JSONObject asobj = associations.getJSONObject(i);
            assocs[i] = new Pair<>(asobj.getString("first_ivorn"), asobj.getString("second_ivorn"));
        }
        return EventDatabase.dump_association2db((Pair<String,String>[]) assocs) != -1;
    }

    @Override
    protected String createURL(SWEKEventType eventType, long start, long end, List<SWEKParam> params, int page) {
        StringBuilder baseURL = new StringBuilder(_baseURL);
        baseURL.append("cmd=search&type=column&");
        baseURL.append("event_type=").append(HEKEventEnum.getHEKEventAbbreviation(eventType.getDisplayName())).append('&');
        baseURL.append("event_coordsys=helioprojective&x1=-3600&x2=3600&y1=-3600&y2=3600&cosec=2&");
        baseURL.append("param0=event_starttime&op0=<=&value0=").append(TimeUtils.format(end)).append('&');
        baseURL = appendParams(baseURL, params);
        baseURL.append("event_starttime=").append(TimeUtils.format(start)).append('&');
        long max = Math.max(System.currentTimeMillis(), end);
        baseURL.append("event_endtime=").append(TimeUtils.format(max)).append('&');
        baseURL.append("page=").append(page);
        return baseURL.toString();
    }

    private static StringBuilder appendParams(StringBuilder baseURL, List<SWEKParam> params) {
        int paramCount = 1;

        for (SWEKParam param : params) {
            if (param.param.toLowerCase().equals("provider")) {
                String encodedValue;
                try {
                    encodedValue = URLEncoder.encode(param.value, StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    encodedValue = param.value;
                }
                baseURL.append("param").append(paramCount).append('=').append("frm_name").append('&').append("op").append(paramCount).append('=').append(param.operand.encodedRepresentation).append('&').append("value").append(paramCount).append('=').append(encodedValue).append('&');
                paramCount++;
            }
        }
        return baseURL;
    }

}
