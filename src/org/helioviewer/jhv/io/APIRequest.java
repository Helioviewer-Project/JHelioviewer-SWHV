package org.helioviewer.jhv.io;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.database.SourcesDatabase;
import org.json.JSONObject;

public class APIRequest {

    private static final long RANGE_EXPAND = 60 * TimeUtils.MINUTE_IN_MILLIS;
    public static final int CADENCE_ANY = -100;
    public static final int CADENCE_DEFAULT = 1800;
    public static final int CallistoID = 5000;

    public final String server;
    public final int sourceId;
    public final long startTime;
    public final long endTime;
    public final int cadence;

    public APIRequest(@Nonnull String _server, int _sourceId, long _startTime, long _endTime, int _cadence) {
        server = _server;
        sourceId = _sourceId;
        cadence = _cadence;

        long expand = (RANGE_EXPAND - (_endTime - _startTime)) / 2;
        if (_startTime != _endTime && expand > 0) {
            startTime = _startTime - expand;
            endTime = _endTime + expand;
        } else {
            startTime = _startTime;
            endTime = _endTime;
        }
    }

    public String toFileRequest() throws IOException {
        String api;
        String fileReq;
        if (startTime == endTime) {
            if ((api = DataSources.getServerSetting(server, "API.getJP2Image")) == null)
                throw new IOException("Unknown server: " + server);
            fileReq = api + "json=true&sourceId=" + sourceId + "&date=" + TimeUtils.formatZ(startTime);
        } else {
            if ((api = DataSources.getServerSetting(server, "API.getJPX")) == null)
                throw new IOException("Unknown server: " + server);
            fileReq = api + "verbose=true&linked=true&sourceId=" + sourceId + "&startTime=" + TimeUtils.formatZ(startTime) + "&endTime=" + TimeUtils.formatZ(endTime);
            if (cadence != CADENCE_ANY) {
                fileReq += "&cadence=" + cadence;
            }
        }
        return fileReq;
    }

    String toJpipRequest() throws IOException {
        return toFileRequest() + "&jpip=true";
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof APIRequest))
            return false;
        APIRequest r = (APIRequest) o;
        return sourceId == r.sourceId && startTime == r.startTime && endTime == r.endTime && cadence == r.cadence && server.equals(r.server);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + sourceId;
        result = 31 * result + cadence;
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + (int) (endTime ^ (endTime >>> 32));
        return 31 * result + server.hashCode();
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

        int _sourceId = SourcesDatabase.doSelect(_server, observatory, dataset);
        if (_sourceId < 0)
            throw new Exception("Empty request result");

        return new APIRequest(_server, _sourceId, _startTime, _endTime, _cadence);
    }

}
