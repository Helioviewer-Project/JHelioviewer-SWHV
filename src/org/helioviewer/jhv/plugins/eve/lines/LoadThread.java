package org.helioviewer.jhv.plugins.eve.lines;

import javax.annotation.Nullable;

import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.band.BandType;
import org.json.JSONObject;

class LoadThread extends JHVWorker<EVEResponse, Void> {

    private final JSONObject jo;

    LoadThread(JSONObject _jo) {
        jo = _jo;
    }

    @Nullable
    @Override
    protected EVEResponse backgroundWork() {
        try {
            return new EVEResponse(jo);
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
                    Timelines.getLayers().add(band);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
