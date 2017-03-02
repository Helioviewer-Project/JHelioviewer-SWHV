package org.helioviewer.jhv.timelines.data;

public interface DataProvider {

    public abstract void updateBand(Band band, long start, long end);

    public abstract void stopDownloads(Band band);

    public abstract boolean isDownloadActive(Band band);
}
