package org.helioviewer.jhv.plugins.eveplugin;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;

import javax.swing.JComponent;

import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.MainContentPanelPlugin;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.events.EventModel;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandTypeAPI;
import org.helioviewer.jhv.plugins.eveplugin.radio.RadioData;
import org.helioviewer.jhv.plugins.eveplugin.view.TimelineDialog;
import org.helioviewer.jhv.plugins.eveplugin.view.chart.PlotPanel;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorTablePanel;
import org.helioviewer.jhv.threads.JHVExecutor;
import org.helioviewer.jhv.threads.JHVWorker;

public class EVEPlugin implements Plugin, MainContentPanelPlugin {

    private static final int MAX_WORKER_THREADS = 12;
    public static final ExecutorService executorService = JHVExecutor.getJHVWorkersExecutorService("EVE", MAX_WORKER_THREADS);

    private final LinkedList<JComponent> pluginPanes = new LinkedList<>();
    private final PlotPanel plotOne = new PlotPanel();

    public static final LineDataSelectorModel ldsm = new LineDataSelectorModel();
    private static final DrawController dc = new DrawController();
    public static final RadioData rd = new RadioData();
    public static final EventModel em = new EventModel();
    public static final TimelineDialog td = new TimelineDialog();

    private static final LineDataSelectorTablePanel timelinePluginPanel = new LineDataSelectorTablePanel();

    public EVEPlugin() {
        LineDataSelectorModel.addLineDataSelectorModelListener(dc);
        LineDataSelectorModel.addLineDataSelectorModelListener(td.getObservationPanel());
        LineDataSelectorModel.addLineData(rd);
        LineDataSelectorModel.addLineData(em);
    }

    @Override
    public void installPlugin() {
        pluginPanes.add(plotOne);

        ImageViewerGui.getLeftContentPane().add("Timeline Layers", timelinePluginPanel, true);
        ImageViewerGui.getLeftContentPane().revalidate();

        ImageViewerGui.getMainContentPanel().addPlugin(this);

        Layers.addLayersListener(dc);
        Layers.addTimeListener(dc);
        Layers.addTimespanListener(dc);
        JHVRelatedEvents.addHighlightListener(dc);

        // em.fetchData(DrawController.selectedAxis);

        JHVWorker<Void, Void> loadSources = new JHVWorker<Void, Void>() {

            @Override
            protected Void backgroundWork() {
                BandTypeAPI.getDatasets();
                return null;
            }

            @Override
            protected void done() {
                td.getObservationPanel().setupDatasets();
            }

        };

        loadSources.setThreadName("EVE--LoadSources");
        executorService.execute(loadSources);
    }

    @Override
    public void uninstallPlugin() {
        JHVRelatedEvents.removeHighlightListener(dc);
        Layers.removeTimespanListener(dc);
        Layers.removeTimeListener(dc);
        Layers.removeLayersListener(dc);

        ImageViewerGui.getMainContentPanel().removePlugin(this);

        ImageViewerGui.getLeftContentPane().remove(timelinePluginPanel);
        ImageViewerGui.getLeftContentPane().revalidate();
        pluginPanes.remove(plotOne);
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
    public LinkedList<JComponent> getVisualInterfaces() {
        return pluginPanes;
    }

    @Override
    public String getTabName() {
        return "Timelines";
    }

    @Override
    public void setState(String state) {
    }

    @Override
    public String getState() {
        return null;
    }

}
