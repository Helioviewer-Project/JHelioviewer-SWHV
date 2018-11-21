package org.helioviewer.jhv.timelines.band;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.TimeUtils;

class BandDownloadTask extends JHVWorker<BandResponse, Void> {

    private final Interval interval;
    private final Band band;

    BandDownloadTask(Band _band, Interval _interval) {
        interval = _interval;
        band = _band;
    }

    @Nullable
    @Override
    protected BandResponse backgroundWork() {
        try {
            return new BandResponse(JSONUtils.get(buildRequest(interval, band.getBandType())));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void done() {
        if (!isCancelled()) {
            try {
                BandResponse r = get();
                if (r != null) {
                    if (!r.bandName.equals(band.getBandType().getName()))
                        throw new Exception("Expected " + band.getBandType().getName() + ", got " + r.bandName);
                    band.addToCache(r.values, r.dates);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        BandDataProvider.downloadFinished(band, interval);
    }

    private static String buildRequest(Interval interval, BandType type) {
        return type.getBaseURL() + "start_date=" + TimeUtils.formatDate(interval.start) + "&end_date=" + TimeUtils.formatDate(interval.end) +
                "&timeline=" + type.getName();
    }

}
