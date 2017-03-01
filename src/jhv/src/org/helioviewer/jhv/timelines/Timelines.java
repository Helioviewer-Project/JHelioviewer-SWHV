package org.helioviewer.jhv.timelines;

import java.util.LinkedList;

import javax.swing.JComponent;

import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.data.cache.JHVRelatedEvents;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.MainContentPanelPlugin;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.view.TimelineDialog;
import org.helioviewer.jhv.timelines.view.chart.PlotPanel;
import org.helioviewer.jhv.timelines.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.timelines.view.linedataselector.LineDataSelectorTablePanel;

public class Timelines implements Plugin, MainContentPanelPlugin {

    public static final LineDataSelectorModel ldsm = new LineDataSelectorModel();
    private static final DrawController dc = new DrawController();
    public static final TimelineDialog td = new TimelineDialog();
    private final LinkedList<JComponent> pluginPanes = new LinkedList<>();
    private final PlotPanel plotOne = new PlotPanel();
    private static final LineDataSelectorTablePanel timelinePluginPanel = new LineDataSelectorTablePanel();

    public Timelines() {
        LineDataSelectorModel.addLineDataSelectorModelListener(dc);
    }

    @Override
    public String getTabName() {
        return "Timelines";
    }

    @Override
    public LinkedList<JComponent> getVisualInterfaces() {
        return pluginPanes;
    }

    @Override
    public String getName() {
        return "Timelines Plugin";
    }

    @Override
    public String getDescription() {
        return "This plugin visualizes 1D and 2D time series";
    }

    @Override
    public void installPlugin() {
        pluginPanes.add(plotOne);

        ImageViewerGui.getLeftContentPane().add("Timeline Layers", timelinePluginPanel, true);
        ImageViewerGui.getLeftContentPane().revalidate();

        ImageViewerGui.getMainContentPanel().addPlugin(this);

        // em.fetchData(DrawController.selectedAxis);

        Layers.addLayersListener(dc);
        Layers.addTimeListener(dc);
        Layers.addTimespanListener(dc);
        JHVRelatedEvents.addHighlightListener(dc);
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
    public void setState(String state) {
    }

    @Override
    public String getState() {
        return null;
    }

    @Override
    public String getAboutLicenseText() {
        return "Mozilla Public License Version 2.0";
    }

}
