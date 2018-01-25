package org.helioviewer.jhv.io;

import java.util.ArrayList;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.database.DataSourcesDB;
import org.json.JSONObject;

public class APIRequest {

    public static final int CADENCE_ANY = -100;
    public static final int CADENCE_DEFAULT = 1800;
    public static final int CallistoID = 5000;

    public final String server;
    public final int sourceId;
    public final long startTime;
    public final long endTime;
    public final int cadence;

    public APIRequest(String _server, int _sourceId, long _startTime, long _endTime, int _cadence) {
        server = _server;
        sourceId = _sourceId;
        startTime = _startTime;
        endTime = _endTime;
        cadence = _cadence;
    }

    public String toFileRequest() {
        String fileReq;
        if (startTime == endTime) {
            fileReq = DataSources.getServerSetting(server, "API.getJP2Image") + "sourceId=" + Integer.toString(sourceId) +
                                                   "&date=" + TimeUtils.formatZ(startTime);
        } else {
            fileReq = DataSources.getServerSetting(server, "API.getJPX") + "sourceId=" + Integer.toString(sourceId) +
                                                   "&startTime=" + TimeUtils.formatZ(startTime) + "&endTime=" + TimeUtils.formatZ(endTime);
            if (cadence != CADENCE_ANY) {
                fileReq += "&cadence=" + Integer.toString(cadence);
            }
        }
        return fileReq;
    }

    String toJpipRequest() {
        return toFileRequest() + "&jpip=true&verbose=true&linked=true";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof APIRequest))
            return false;
        APIRequest r = (APIRequest) o;
        return sourceId == r.sourceId && startTime == r.startTime && endTime == r.endTime && cadence == r.cadence && server.equals(r.server);
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

    public JSONObject toJson() {
        JSONObject jo = new JSONObject();
        jo.put("server", server);
        jo.put("sourceId", sourceId);
        jo.put("startTime", TimeUtils.format(startTime));
        jo.put("endTime", TimeUtils.format(endTime));
        jo.put("cadence", cadence);
        return jo;
    }

    private static final int MAX_FRAMES = 99;

    public static APIRequest fromJson(JSONObject jo) {
        String _server = jo.optString("server", Settings.getSingletonInstance().getProperty("default.server"));
        int _sourceId = jo.optInt("sourceId", 10);

        long t = System.currentTimeMillis();
        long _startTime = TimeUtils.optParse(jo.optString("startTime"), t - 2 * TimeUtils.DAY_IN_MILLIS);
        long _endTime = TimeUtils.optParse(jo.optString("endTime"), t);

        int _cadence = jo.optInt("cadence", (int) Math.max(1, (_endTime - _startTime) / 1000 / MAX_FRAMES));
        return new APIRequest(_server, _sourceId, _startTime, _endTime, _cadence);
    }

    public static APIRequest fromRequestJson(JSONObject jo) throws Exception {
        long t = System.currentTimeMillis();
        long _startTime = TimeUtils.optParse(jo.optString("startTime"), t - 2 * TimeUtils.DAY_IN_MILLIS);
        long _endTime = TimeUtils.optParse(jo.optString("endTime"), t);

        int _cadence = jo.optInt("cadence", (int) Math.max(1, (_endTime - _startTime) / 1000 / MAX_FRAMES));

        String observatory = jo.optString("observatory", "");
        String dataset = jo.getString("dataset");
        ArrayList<Pair<Integer, String>> res = DataSourcesDB.doSelect(Settings.getSingletonInstance().getProperty("default.server"), observatory, dataset);
        if (res.isEmpty())
            throw new Exception("Empty request result");

        int _sourceId = res.get(0).a;
        String _server = res.get(0).b;

        return new APIRequest(_server, _sourceId, _startTime, _endTime, _cadence);
    }

}
