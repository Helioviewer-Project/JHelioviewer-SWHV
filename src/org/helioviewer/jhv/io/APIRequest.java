package org.helioviewer.jhv.io;

import java.net.URI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Settings;
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

    public APIRequest withSpan(long start, long end, int _cadence) {
        return new APIRequest(server, sourceId, start, end, _cadence);
    }

    public String toFileRequest() throws Exception {
        DataSources.Server source = DataSources.getServer(server);
        if (source == null)
            throw new Exception("Unknown server: " + server);

        String fileReq;
        if (startTime == endTime) {
            fileReq = source.jp2URL() + "sourceId=" + sourceId + "&date=" + TimeUtils.formatZ(startTime);
        } else {
            fileReq = source.jpxURL() + "sourceId=" + sourceId + "&startTime=" + TimeUtils.formatZ(startTime) + "&endTime=" + TimeUtils.formatZ(endTime);
            if (cadence != CADENCE_ALL)
                fileReq += "&cadence=" + cadence;
        }
        return fileReq;
    }

    public String toJpipRequest() throws Exception {
        String jsonReq = startTime == endTime ? "&json=true" : "&verbose=true&linked=true";
        return toFileRequest() + jsonReq + "&jpip=true";
    }

    // The server's answer: the JPIP URI to open, and a message for the user if it sent one.
    public record Response(URI uri, @Nullable String message) {}

    public Response resolve() throws Exception {
        String url = toJpipRequest();
        try {
            JSONObject data = JSONUtils.get(new URI(url));
            if (!data.isNull("frames"))
                data.put("frames", data.getJSONArray("frames").length()); // don't log timestamps, modifies input
            Log.info(data.toString());
            String error = data.optString("error", null);
            if (error != null)
                throw new Exception(error);
            return new Response(new URI(data.getString("uri")), data.optString("message", null));
        } catch (Exception e) {
            throw new Exception("Invalid response for " + url + ": " + e.getMessage(), e);
        }
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
        if (DataSources.getServer(_server) == null)
            _server = Settings.getProperty("dataSources.defaultServer");

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
        if (DataSources.getServer(_server) == null)
            _server = Settings.getProperty("dataSources.defaultServer");
        if (DataSources.getServer(_server) == null) // very unlikely
            throw new Exception("Unknown server");

        int _sourceId = DataSources.selectDataset(_server, observatory, dataset);
        if (_sourceId < 0)
            throw new Exception("Empty request result");

        return new APIRequest(_server, _sourceId, _startTime, _endTime, _cadence);
    }

}
