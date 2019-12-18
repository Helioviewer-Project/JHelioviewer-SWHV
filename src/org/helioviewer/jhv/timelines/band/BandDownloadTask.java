package org.helioviewer.jhv.timelines.band;

import javax.annotation.Nullable;

import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.TimeUtils;

class BandDownloadTask extends JHVWorker<BandResponse, Void> {

    final Band band;
    private final long startTime;
    private final long endTime;

    BandDownloadTask(Band _band, long _startTime, long _endTime) {
        band = _band;
        startTime = _startTime;
        endTime = _endTime;
    }

    @Nullable
    @Override
    protected BandResponse backgroundWork() {
        try {
            BandType type = band.getBandType();
            String request = type.getBaseURL() + "start_date=" + TimeUtils.formatDate(startTime) + "&end_date=" + TimeUtils.formatDate(endTime) +
                "&timeline=" + type.getName();
            return new BandResponse(JSONUtils.get(request));
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
        BandDataProvider.downloadFinished(this);
    }

}
