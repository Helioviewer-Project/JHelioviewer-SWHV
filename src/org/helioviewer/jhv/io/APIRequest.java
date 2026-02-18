package org.helioviewer.jhv.io;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONObject;

public record APIRequest(@Nonnull String server, int sourceId, long startTime, long endTime, int cadence) {

    private static final long RANGE_EXPAND = 15 * TimeUtils.MINUTE_IN_MILLIS;
    public static final int CADENCE_ALL = -100;
    public static final int CADENCE_DEFAULT = 1800;
    public static final int CallistoID = 5000;

    public APIRequest {
        if (endTime < startTime)
            endTime = startTime;

        long expand = (RANGE_EXPAND - (endTime - startTime)) / 2;
        if (startTime != endTime && expand > 0) {
            startTime = startTime - expand;
            endTime = endTime + expand;
        }
    }

    public String toFileRequest() throws Exception {
        String api;
        String fileReq;
        if (startTime == endTime) {
            if ((api = DataSources.getServerSetting(server, "API.getJP2Image")) == null)
                throw new Exception("Unknown server: " + server);
            fileReq = api + "sourceId=" + sourceId + "&date=" + TimeUtils.formatZ(startTime);
        } else {
            if ((api = DataSources.getServerSetting(server, "API.getJPX")) == null)
                throw new Exception("Unknown server: " + server);
            fileReq = api + "sourceId=" + sourceId + "&startTime=" + TimeUtils.formatZ(startTime) + "&endTime=" + TimeUtils.formatZ(endTime);
            if (cadence != CADENCE_ALL)
                fileReq += "&cadence=" + cadence;
        }
        return fileReq;
    }

    String toJpipUrl() throws Exception {
        String jsonReq = startTime == endTime ? "&json=true" : "&verbose=true&linked=true";
        return toFileRequest() + jsonReq + "&jpip=true";
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

    public static APIRequest fromJson(JSONObject jo) {
        String _server = jo.optString("server", "");
        if (DataSources.getServerSetting(_server, "API.getDataSources") == null)
            _server = Settings.getProperty("default.server");

        int _sourceId = jo.optInt("sourceId", 10);

        long t = System.currentTimeMillis();
        long _startTime = TimeUtils.optParse(jo.optString("startTime"), t - 2 * TimeUtils.DAY_IN_MILLIS);
        long _endTime = TimeUtils.optParse(jo.optString("endTime"), t);

        int _cadence = jo.optInt("cadence", TimeUtils.defaultCadence(_startTime, _endTime));
        return new APIRequest(_server, _sourceId, _startTime, _endTime, _cadence);
    }

    public static APIRequest fromRequestJson(JSONObject jo) throws Exception {
        long t = System.currentTimeMillis();
        long _startTime = TimeUtils.optParse(jo.optString("startTime"), t - 2 * TimeUtils.DAY_IN_MILLIS);
        long _endTime = TimeUtils.optParse(jo.optString("endTime"), t);
        int _cadence = jo.optInt("cadence", TimeUtils.defaultCadence(_startTime, _endTime));

        String observatory = jo.optString("observatory", "");
        String dataset = jo.getString("dataset");

        String _server = jo.optString("server", "");
        if (DataSources.getServerSetting(_server, "API.getDataSources") == null)
            _server = Settings.getProperty("default.server");
        if (_server == null || DataSources.getServerSetting(_server, "API.getDataSources") == null) // very unlikely
            throw new Exception("Unknown server");

        int _sourceId = DataSources.select(_server, observatory, dataset);
        if (_sourceId < 0)
            throw new Exception("Empty request result");

        return new APIRequest(_server, _sourceId, _startTime, _endTime, _cadence);
    }

}
