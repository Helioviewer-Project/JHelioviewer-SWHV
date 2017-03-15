package org.helioviewer.jhv.timelines.data;

public interface DataProvider {

    void updateBand(Band band, long start, long end);

    void stopDownloads(Band band);

    boolean isDownloadActive(Band band);

}
