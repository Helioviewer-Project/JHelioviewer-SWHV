package org.helioviewer.jhv.io;

import org.helioviewer.jhv.base.time.TimeUtils;

public class APIRequest {

    public static final int CADENCE_ANY = -100;

    public final String server;
    public final int sourceId;
    public final long startTime;
    public final long endTime;
    public final int cadence;

    public final String jpipRequest;
    public final String fileRequest;

    public APIRequest(String server, int sourceId, long startTime, long endTime, int cadence) {
        this.server = server;
        this.sourceId = sourceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.cadence = cadence;

        if (startTime == endTime) {
            fileRequest = DataSources.getServerSetting(server, "API.jp2images.path") + "sourceId=" + Integer.toString(sourceId) +
                                                       "&date=" + TimeUtils.apiDateFormat.format(startTime) + "&json=true";
            jpipRequest = fileRequest + "&jpip=true";
        } else {
            String fileReq = DataSources.getServerSetting(server, "API.jp2series.path") + "sourceId=" + Integer.toString(sourceId) +
                                                          "&startTime=" + TimeUtils.apiDateFormat.format(startTime) + "&endTime=" + TimeUtils.apiDateFormat.format(endTime);
            if (cadence != CADENCE_ANY) {
                fileReq += "&cadence=" + Integer.toString(cadence);
            }
            fileRequest = fileReq;
            jpipRequest = fileRequest + "&jpip=true&verbose=true&linked=true";
        }
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
