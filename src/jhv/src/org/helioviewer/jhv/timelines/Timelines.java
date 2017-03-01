package org.helioviewer.jhv.timelines;

import java.util.LinkedList;

import javax.swing.JComponent;

import org.helioviewer.jhv.data.cache.JHVRelatedEvents;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.view.TimelineDialog;
import org.helioviewer.jhv.timelines.view.chart.PlotPanel;
import org.helioviewer.jhv.timelines.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.timelines.view.linedataselector.LineDataSelectorTablePanel;

public class Timelines {

    public static final LineDataSelectorModel ldsm = new LineDataSelectorModel();
    private static final DrawController dc = new DrawController();
    public static final TimelineDialog td = new TimelineDialog();
    private final LinkedList<JComponent> pluginPanes = new LinkedList<>();
    private final PlotPanel plotOne = new PlotPanel();
    private static final LineDataSelectorTablePanel timelinePanel = new LineDataSelectorTablePanel();

    public Timelines() {
        LineDataSelectorModel.addLineDataSelectorModelListener(dc);
    }

    public void installTimelines() {
        pluginPanes.add(plotOne);

        ImageViewerGui.getLeftContentPane().add("Timeline Layers", timelinePanel, true);
        ImageViewerGui.getLeftContentPane().revalidate();
        ComponentUtils.setVisible(plotOne, true);
        ImageViewerGui.getMainContentPanel().updateLayout();
        // em.fetchData(DrawController.selectedAxis);

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

        ComponentUtils.setVisible(plotOne, false);
        ImageViewerGui.getMainContentPanel().updateLayout();

        ImageViewerGui.getLeftContentPane().remove(timelinePanel);
        ImageViewerGui.getLeftContentPane().revalidate();
        pluginPanes.remove(plotOne);
    }
}
