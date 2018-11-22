package org.helioviewer.jhv.timelines.band;

import javax.annotation.Nullable;

import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.TimelineSettings;

class BandTypeTask extends JHVWorker<Void, Void> {

    BandTypeTask() {
        setThreadName("EVE--LoadSources");
    }

    @Nullable
    @Override
    protected Void backgroundWork() {
        try {
            BandType.loadBandTypes(JSONUtils.get(TimelineSettings.baseURL).getJSONArray("objects"));
        } catch (Exception e) {
            Log.error("Error loading bandtypes", e);
        }
        return null;
    }

    @Override
    protected void done() {
        Timelines.td.getObservationPanel().setupDatasets();
    }

}
