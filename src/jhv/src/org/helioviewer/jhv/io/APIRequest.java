package org.helioviewer.jhv.io;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;

public class APIRequest {

    public static final int CADENCE_ANY = -100;

    public final String server;
    public final int sourceId;
    public final long startTime;
    public final long endTime;
    public final int cadence;

    public final URL jpipRequest;
    public final URI fileRequest;

    public APIRequest(String server, int sourceId, long startTime, long endTime, int cadence) {
        this.server = server;
        this.sourceId = sourceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.cadence = cadence;

        String jpipReq, fileReq;
        if (startTime == endTime) {
            fileReq = DataSources.getServerSetting(server, "API.jp2images.path") + "sourceId=" + Integer.toString(sourceId) +
                                                   "&date=" + TimeUtils.apiDateFormat.format(startTime) + "&json=true";
            jpipReq = fileReq + "&jpip=true";
        } else {
            fileReq = DataSources.getServerSetting(server, "API.jp2series.path") + "sourceId=" + Integer.toString(sourceId) +
                                                   "&startTime=" + TimeUtils.apiDateFormat.format(startTime) + "&endTime=" + TimeUtils.apiDateFormat.format(endTime);
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
        if (o instanceof APIRequest) {
            APIRequest r = (APIRequest) o;
            return sourceId == r.sourceId && startTime == r.startTime && endTime == r.endTime && cadence == r.cadence && server.equals(r.server);
        }
        return false;
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

}
