package org.helioviewer.jhv.timelines.band;

import org.json.JSONObject;

public interface BandDataProvider {

    void loadBand(JSONObject jo);

    void updateBand(Band band, long start, long end);

    void stopDownloads(Band band);

    boolean isDownloadActive(Band band);

}
