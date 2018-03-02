package org.helioviewer.jhv.timelines.band;

import org.helioviewer.jhv.io.LoadJSON;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.TimelineSettings;

public class BandTypeTask extends JHVWorker<Void, Void> {

    public BandTypeTask() {
        setThreadName("EVE--LoadSources");
    }

    @Override
    protected Void backgroundWork() {
       try {
            BandType.loadBandTypes(LoadJSON.of(TimelineSettings.baseURL).getJSONArray("objects"));
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
