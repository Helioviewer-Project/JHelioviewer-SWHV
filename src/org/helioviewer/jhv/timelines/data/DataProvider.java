package org.helioviewer.jhv.timelines.data;

import java.net.URI;

public interface DataProvider {

    void loadBand(URI uri);

    void updateBand(Band band, long start, long end);

    void stopDownloads(Band band);

    boolean isDownloadActive(Band band);

}
