package org.helioviewer.jhv.timelines.band;

import javax.annotation.Nullable;

import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.timelines.Timelines;
import org.json.JSONObject;

class BandLoadTask extends JHVWorker<BandResponse, Void> {

    private final JSONObject jo;

    BandLoadTask(JSONObject _jo) {
        jo = _jo;
    }

    @Nullable
    @Override
    protected BandResponse backgroundWork() {
        try {
            return new BandResponse(jo);
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
