package org.helioviewer.jhv.timelines.band;

public interface BandDataProvider {

    void updateBand(Band band, long start, long end);

    void stopDownloads(Band band);

    boolean isDownloadActive(Band band);

}
