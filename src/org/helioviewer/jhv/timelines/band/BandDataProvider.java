package org.helioviewer.jhv.timelines.band;

import java.net.URI;

public interface BandDataProvider {

    void loadBand(URI uri);

    void updateBand(Band band, long start, long end);

    void stopDownloads(Band band);

    boolean isDownloadActive(Band band);

}
