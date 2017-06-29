package org.helioviewer.jhv.plugins.eve;

import java.util.concurrent.ExecutorService;

import javax.swing.JMenuItem;

import org.helioviewer.jhv.base.plugin.Plugin;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.plugins.eve.lines.EVEDataProvider;
import org.helioviewer.jhv.plugins.eve.radio.RadioData;
import org.helioviewer.jhv.threads.JHVExecutor;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.band.BandTypeTask;
import org.helioviewer.jhv.timelines.gui.NewLayerAction;
import org.helioviewer.jhv.timelines.gui.OpenLocalFileAction;
import org.json.JSONObject;

public class EVEPlugin implements Plugin {

    private static final int MAX_WORKER_THREADS = 12;
    public static final ExecutorService executorService = JHVExecutor.createJHVWorkersExecutorService("EVE", MAX_WORKER_THREADS);
    public static final EVEDataProvider eveDataprovider = new EVEDataProvider();

    private final Timelines tl = new Timelines();
    private final JMenuItem newItem = new JMenuItem(new NewLayerAction());
    private final JMenuItem openItem = new JMenuItem(new OpenLocalFileAction());

    public EVEPlugin() {
        Timelines.getModel().addLineData(new RadioData());
    }

    @Override
    public void installPlugin() {
        tl.installTimelines();
        executorService.execute(new BandTypeTask());
        ImageViewerGui.getMenuBar().getMenu(0).add(newItem, 1);
        ImageViewerGui.getMenuBar().getMenu(0).add(openItem, 3);
    }

    @Override
    public void uninstallPlugin() {
        tl.uninstallTimelines();
        ImageViewerGui.getMenuBar().getMenu(0).remove(newItem);
        ImageViewerGui.getMenuBar().getMenu(0).remove(openItem);
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
