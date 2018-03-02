package org.helioviewer.jhv.plugins.eve.lines;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.io.LoadJSON;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.band.BandType;

class DownloadThread extends JHVWorker<EVEResponse, Void> {

    private final Interval interval;
    private final Band band;

    DownloadThread(Band _band, Interval _interval) {
        interval = _interval;
        band = _band;
    }

    @Override
    protected EVEResponse backgroundWork() {
        try {
            return new EVEResponse(LoadJSON.of(buildRequest(interval, band.getBandType())));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void done() {
        if (!isCancelled()) {
            try {
                EVEResponse r = get();
                if (r != null) {
                    if (!r.bandName.equals(band.getBandType().getName()))
                        throw new Exception("Expected " + band.getBandType().getName() + ", got " + r.bandName);
                    band.addToCache(r.values, r.dates);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        EVEDataProvider.downloadFinished(band, interval);
    }

    private static String buildRequest(Interval interval, BandType type) {
        return type.getBaseURL() + "start_date=" + TimeUtils.formatDate(interval.start) + "&end_date=" + TimeUtils.formatDate(interval.end) +
               "&timeline=" + type.getName();
    }

}
