package org.helioviewer.jhv.plugins.eve;

import java.util.concurrent.ExecutorService;

import org.helioviewer.jhv.base.plugin.Plugin;
import org.helioviewer.jhv.plugins.eve.lines.BandTypeAPI;
import org.helioviewer.jhv.plugins.eve.radio.RadioData;
import org.helioviewer.jhv.plugins.eve.view.TimelineDataPanel;
import org.helioviewer.jhv.threads.JHVExecutor;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.timelines.Timelines;
import org.json.JSONObject;

public class EVEPlugin implements Plugin {

    private static final int MAX_WORKER_THREADS = 12;
    public static final ExecutorService executorService = JHVExecutor.getJHVWorkersExecutorService("EVE", MAX_WORKER_THREADS);

    private final Timelines tl = new Timelines();

    public EVEPlugin() {
        Timelines.getModel().addLineData(new RadioData());
    }

    @Override
    public void installPlugin() {
        tl.installTimelines();

        Timelines.td.setObservationPanel(new TimelineDataPanel());
        JHVWorker<Void, Void> loadSources = new JHVWorker<Void, Void>() {

            @Override
            protected Void backgroundWork() {
                BandTypeAPI.getDatasets();
                return null;
            }

            @Override
            protected void done() {
                Timelines.td.getObservationPanel().setupDatasets();
            }

        };

        loadSources.setThreadName("EVE--LoadSources");
        executorService.execute(loadSources);
    }

    @Override
    public void uninstallPlugin() {
        tl.uninstallTimelines();
    }

    @Override
    public String getName() {
        return "Timeline Plugin";
    }

    @Override
    public String getDescription() {
        return "This plugin visualizes 1D and 2D time series";
    }

    @Override
    public String getAboutLicenseText() {
        return "Mozilla Public License Version 2.0";
    }

    @Override
    public void saveState(JSONObject jo) {
    }

    @Override
    public void loadState(JSONObject jo) {
    }

}
