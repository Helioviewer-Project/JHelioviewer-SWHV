package org.helioviewer.jhv.timelines;

import java.util.LinkedList;

import javax.swing.JComponent;

import org.helioviewer.jhv.data.cache.JHVRelatedEvents;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.MainContentPanelPlugin;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.view.TimelineDialog;
import org.helioviewer.jhv.timelines.view.chart.PlotPanel;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelinePanel;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineTableModel;

public class Timelines implements MainContentPanelPlugin {

    public static final TimelineTableModel ldsm = new TimelineTableModel();
    private static final DrawController dc = new DrawController();
    public static final TimelineDialog td = new TimelineDialog();
    private final LinkedList<JComponent> pluginPanes = new LinkedList<>();
    private final PlotPanel plotOne = new PlotPanel();
    private static final TimelinePanel timelinePanel = new TimelinePanel();

    public Timelines() {
        TimelineTableModel.addLineDataSelectorModelListener(dc);
    }

    public void installTimelines() {
        pluginPanes.add(plotOne);

        ImageViewerGui.getLeftContentPane().add("Timeline Layers", timelinePanel, true);
        ImageViewerGui.getLeftContentPane().revalidate();
        // em.fetchData(DrawController.selectedAxis);

        ImageViewerGui.getMainContentPanel().addPlugin(this);

        Layers.addLayersListener(dc);
        Layers.addTimeListener(dc);
        Layers.addTimespanListener(dc);
        JHVRelatedEvents.addHighlightListener(dc);
    }

    public void uninstallTimelines() {
        JHVRelatedEvents.removeHighlightListener(dc);
        Layers.removeTimespanListener(dc);
        Layers.removeTimeListener(dc);
        Layers.removeLayersListener(dc);

        ImageViewerGui.getMainContentPanel().removePlugin(this);

        ImageViewerGui.getLeftContentPane().remove(timelinePanel);
        ImageViewerGui.getLeftContentPane().revalidate();
        pluginPanes.remove(plotOne);
    }

    @Override
    public String getTabName() {
        return "Timelines";
    }

    @Override
    public LinkedList<JComponent> getVisualInterfaces() {
        return pluginPanes;
    }

}
