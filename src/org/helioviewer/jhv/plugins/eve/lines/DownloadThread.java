package org.helioviewer.jhv.plugins.eve.lines;

import java.net.URI;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.data.Band;
import org.helioviewer.jhv.timelines.data.BandType;

class DownloadThread extends JHVWorker<EVEResponse, Void> {

    private final Interval interval;
    private final Band band;

    public DownloadThread(Band _band, Interval _interval) {
        interval = _interval;
        band = _band;
    }

    public Interval getInterval() {
        return interval;
    }

    public Band getBand() {
        return band;
    }

    @Override
    protected EVEResponse backgroundWork() {
        try {
            return EVEResponse.get(new URI(buildRequest(interval, band.getBandType())));
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
