package org.helioviewer.jhv.timelines.data;

import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.timelines.Timelines;

public class BandTypeTask extends JHVWorker<Void, Void> {

    public BandTypeTask() {
        setThreadName("EVE--LoadSources");
    }

    @Override
    protected Void backgroundWork() {
        BandTypeAPI.getDatasets();
        return null;
    }

    @Override
    protected void done() {
        Timelines.td.getObservationPanel().setupDatasets();
    }

}
