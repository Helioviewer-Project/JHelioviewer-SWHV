package org.helioviewer.jhv.timelines;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.helioviewer.jhv.event.JHVEventCache;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.timelines.chart.PlotPanel;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.gui.TimelineDialog;
import org.helioviewer.jhv.timelines.gui.TimelinePanel;
import org.helioviewer.jhv.timelines.radio.RadioData;

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
        MainFrame.getLeftContentPane().add("Timeline Layers", timelinePanel, true);
        MainFrame.getLeftContentPane().revalidate();
        MainFrame.getMainContentPanel().addPlugin(this);

        Player.addTimeListener(dc);
        JHVEventCache.addHighlightListener(dc);
    }

    public void uninstallTimelines() {
        JHVEventCache.removeHighlightListener(dc);
        Player.removeTimeListener(dc);

        MainFrame.getMainContentPanel().removePlugin(this);
        MainFrame.getLeftContentPane().remove(timelinePanel);
        MainFrame.getLeftContentPane().revalidate();
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
