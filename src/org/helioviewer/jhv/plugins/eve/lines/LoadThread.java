package org.helioviewer.jhv.plugins.eve.lines;

import java.net.URI;

import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.band.BandType;

class LoadThread extends JHVWorker<EVEResponse, Void> {

    private final URI uri;

    LoadThread(URI _uri) {
        uri = _uri;
    }

    @Override
    protected EVEResponse backgroundWork() {
        try {
            return EVEResponse.get(uri);
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
                    Band band = new Band(r.bandType == null ? BandType.getBandType(r.bandName) : r.bandType);
                    band.addToCache(r.values, r.dates);
                    Timelines.getModel().addLineData(band);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
