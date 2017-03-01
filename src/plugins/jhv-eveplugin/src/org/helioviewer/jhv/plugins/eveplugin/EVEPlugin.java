package org.helioviewer.jhv.plugins.eveplugin;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;

import javax.swing.JComponent;

import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.gui.interfaces.MainContentPanelPlugin;
import org.helioviewer.jhv.plugins.eveplugin.events.EventModel;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandTypeAPI;
import org.helioviewer.jhv.plugins.eveplugin.radio.RadioData;
import org.helioviewer.jhv.plugins.eveplugin.view.TimelineDataPanel;
import org.helioviewer.jhv.threads.JHVExecutor;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.view.linedataselector.LineDataSelectorModel;

public class EVEPlugin implements Plugin, MainContentPanelPlugin {

    private static final int MAX_WORKER_THREADS = 12;
    public static final ExecutorService executorService = JHVExecutor.getJHVWorkersExecutorService("EVE", MAX_WORKER_THREADS);

    public static final RadioData rd = new RadioData();
    public static final EventModel em = new EventModel();

    private final Timelines tl = new Timelines();

    public EVEPlugin() {
        LineDataSelectorModel.addLineData(rd);
        LineDataSelectorModel.addLineData(em);
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
        return "Timelines, callisto and event displayer Plugin";
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
    public LinkedList<JComponent> getVisualInterfaces() {
        return null;
    }

    @Override
    public String getTabName() {
        return "Timelines, callisto and event displayer";
    }

    @Override
    public void setState(String state) {
    }

    @Override
    public String getState() {
        return null;
    }

}
