package org.helioviewer.jhv.timelines;

import java.util.LinkedList;

import javax.swing.JComponent;

import org.helioviewer.jhv.data.cache.JHVRelatedEvents;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.MainContentPanelPlugin;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.radio.RadioData;
import org.helioviewer.jhv.timelines.view.TimelineDialog;
import org.helioviewer.jhv.timelines.view.chart.PlotPanel;
import org.helioviewer.jhv.timelines.view.selector.TimelinePanel;
import org.json.JSONObject;

public class Timelines implements MainContentPanelPlugin {

    private static final TimelineLayers layers = new TimelineLayers();
    private static final DrawController dc = new DrawController();
    public static final TimelineDialog td = new TimelineDialog();
    private final LinkedList<JComponent> pluginPanes = new LinkedList<>();
    private final PlotPanel plotOne = new PlotPanel();
    private static final TimelinePanel timelinePanel = new TimelinePanel(layers);

    public Timelines() {
        layers.add(new RadioData(null));
    }

    public static TimelineLayers getLayers() {
        return layers;
    }

    public void installTimelines() {
        pluginPanes.add(plotOne);
        ImageViewerGui.getLeftContentPane().add("Timeline Layers", timelinePanel, true);
        ImageViewerGui.getLeftContentPane().revalidate();
        ImageViewerGui.getMainContentPanel().addPlugin(this);

        Movie.addTimeListener(dc);
        Movie.addTimespanListener(dc);
        JHVRelatedEvents.addHighlightListener(dc);
    }

    public void uninstallTimelines() {
        JHVRelatedEvents.removeHighlightListener(dc);
        Movie.removeTimespanListener(dc);
        Movie.removeTimeListener(dc);

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

    public static void saveState(JSONObject jo) {
        DrawController.saveState(jo);
    }

    public static void loadState(JSONObject jo) {
        DrawController.loadState(jo);
    }

}
