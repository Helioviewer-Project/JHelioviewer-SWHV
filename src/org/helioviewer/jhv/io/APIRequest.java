package org.helioviewer.jhv.io;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
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

    final URL jpipRequest;
    final URI fileRequest;

    public APIRequest(String _server, int _sourceId, long _startTime, long _endTime, int _cadence) {
        server = _server;
        sourceId = _sourceId;
        startTime = _startTime;
        endTime = _endTime;
        cadence = _cadence;

        String jpipReq, fileReq;
        if (startTime == endTime) {
            fileReq = DataSources.getServerSetting(server, "API.getJP2Image") + "sourceId=" + Integer.toString(sourceId) +
                                                   "&date=" + TimeUtils.formatZ(startTime) + "&json=true";
            jpipReq = fileReq + "&jpip=true";
        } else {
            fileReq = DataSources.getServerSetting(server, "API.getJPX") + "sourceId=" + Integer.toString(sourceId) +
                                                   "&startTime=" + TimeUtils.formatZ(startTime) + "&endTime=" + TimeUtils.formatZ(endTime);
            if (cadence != CADENCE_ANY) {
                fileReq += "&cadence=" + Integer.toString(cadence);
            }
            jpipReq = fileReq + "&jpip=true&verbose=true&linked=true";
        }

        URL url = null;
        try {
            url = new URL(jpipReq);
        } catch (MalformedURLException e) {
            Log.error("Malformed JPIP request URL: " + jpipReq);
        }
        jpipRequest = url;

        URI uri = null;
        try {
            uri = new URI(fileReq);
        } catch (URISyntaxException e) {
            Log.error("URI syntax exception: " + fileReq);
        }
        fileRequest = uri;
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
        JSONObject json = new JSONObject();
        json.put("server", server);
        json.put("sourceId", sourceId);
        json.put("startTime", startTime);
        json.put("endTime", endTime);
        json.put("cadence", cadence);
        return json;
    }

    public static APIRequest fromJson(JSONObject json) {
        String _server = json.optString("server", "ROB");
        int _sourceId = json.optInt("sourceId", 10);
        long _startTime = json.optLong("startTime", System.currentTimeMillis());
        long _endTime = json.optLong("endTime", System.currentTimeMillis());
        int _cadence = json.optInt("cadence", CADENCE_DEFAULT);
        return new APIRequest(_server, _sourceId, _startTime, _endTime, _cadence);
    }

}
