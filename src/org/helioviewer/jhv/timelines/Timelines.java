package org.helioviewer.jhv.timelines;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.helioviewer.jhv.events.JHVRelatedEvents;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.timelines.chart.PlotPanel;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.radio.RadioData;
import org.helioviewer.jhv.timelines.gui.TimelineDialog;
import org.helioviewer.jhv.timelines.gui.TimelinePanel;
import org.json.JSONObject;

public class Timelines implements Interfaces.MainContentPanelPlugin {

    private static final TimelineLayers layers = new TimelineLayers();
    public static final DrawController dc = new DrawController(); // sucks
    public static final TimelineDialog td = new TimelineDialog();
    private final List<JComponent> pluginPanes = new ArrayList<>();
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
        JHVFrame.getLeftContentPane().add("Timeline Layers", timelinePanel, true);
        JHVFrame.getLeftContentPane().revalidate();
        JHVFrame.getMainContentPanel().addPlugin(this);

        Movie.addTimeListener(dc);
        JHVRelatedEvents.addHighlightListener(dc);
    }

    public void uninstallTimelines() {
        JHVRelatedEvents.removeHighlightListener(dc);
        Movie.removeTimeListener(dc);

        JHVFrame.getMainContentPanel().removePlugin(this);
        JHVFrame.getLeftContentPane().remove(timelinePanel);
        JHVFrame.getLeftContentPane().revalidate();
        pluginPanes.remove(plotOne);
    }

    @Override
    public String getTabName() {
        return "Timelines";
    }

    @Override
    public List<JComponent> getVisualInterfaces() {
        return pluginPanes;
    }

    public static void saveState(JSONObject jo) {
        DrawController.saveState(jo);
    }

    public static void loadState(JSONObject jo) {
        DrawController.loadState(jo);
    }

}
