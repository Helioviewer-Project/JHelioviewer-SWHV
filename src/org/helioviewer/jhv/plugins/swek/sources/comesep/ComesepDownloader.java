package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.event.JHVEventType;
import org.helioviewer.jhv.data.event.SWEKDownloader;
import org.helioviewer.jhv.data.event.SWEKEventType;
import org.helioviewer.jhv.data.event.SWEKParam;
import org.helioviewer.jhv.database.EventDatabase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unchecked")
public class ComesepDownloader extends SWEKDownloader {

    private static final String _baseurl = "http://swhv.oma.be/comesep/comeseprequestapi/getComesep.php?";

    @Override
    protected boolean parseEvents(JSONObject eventJSON, JHVEventType type) {
        JSONArray results = eventJSON.getJSONArray("results");
        try {
            ArrayList<EventDatabase.Event2Db> event2db_list = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);

                long start = result.getLong("atearliest") * 1000;
                long end = result.getLong("atlatest") * 1000;
                if (result.has("liftoffduration_value")) {
                    long cactusLiftOff = result.getLong("liftoffduration_value");
                    end += cactusLiftOff * 60000;
                }

                byte[] compressed;
                try {
                    compressed = JSONUtils.compressJSON(result);
                } catch (IOException e) {
                    Log.error("compression error");
                    return false;
                }

                long archiv = start;
                String uid = result.getString("alertid");
                event2db_list.add(new EventDatabase.Event2Db(compressed, start, end, archiv, uid, new ArrayList<>()));
            }

            int[] ids = EventDatabase.dump_event2db(event2db_list, type);
            for (int id : ids) {
                if (id == -1) {
                    Log.error("failed to dump to database");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected boolean parseAssociations(JSONObject eventJSON) {
        JSONArray associations = eventJSON.getJSONArray("associations");
        int len = associations.length();
        Pair<?,?>[] assocs = new Pair<?,?>[len];
        for (int i = 0; i < len; i++) {
            JSONObject asobj = associations.getJSONObject(i);
            assocs[i] = new Pair<>(asobj.getString("parent"), asobj.getString("child"));
        }
        return EventDatabase.dump_association2db((Pair<String,String>[]) assocs) != -1;
    }

    @Override
    protected String createURL(SWEKEventType eventType, long start, long end, List<SWEKParam> params, int page) {
        StringBuilder baseURL = new StringBuilder(_baseurl);
        baseURL = appendModel(baseURL, params).append('&');
        baseURL.append("startdate=").append(TimeUtils.format(start)).append('&');
        baseURL.append("enddate=").append(TimeUtils.format(end)).append('&');
        return baseURL.toString();
    }

    private static StringBuilder appendModel(StringBuilder baseURL, List<SWEKParam> params) {
        String model = "";
        for (SWEKParam p : params) {
            if (p.param.equals("provider")) {
                model = p.value;
                break;
            }
        }
        return baseURL.append("model=").append(model);
    }

}
