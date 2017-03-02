package org.helioviewer.jhv.timelines.data;

import org.helioviewer.jhv.timelines.view.linedataselector.TimelineTableModel;

public class DownloadController {

    public static void updateBand(Band band, long start, long end) {
        band.getBandType().getDataprovider().updateBand(band, start, end);
    }

    public static void stopDownloads(Band band) {
        band.getBandType().getDataprovider().stopDownloads(band);
    }

    public static boolean isDownloadActive(Band band) {
        return band.getBandType().getDataprovider().isDownloadActive(band);
    }

    public static void fireDownloadStarted(Band band) {
        TimelineTableModel.downloadStarted(band);
    }

    public static void fireDownloadFinished(Band band) {
        TimelineTableModel.downloadFinished(band);
    }

}
