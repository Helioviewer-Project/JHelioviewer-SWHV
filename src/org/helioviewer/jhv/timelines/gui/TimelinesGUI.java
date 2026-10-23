package org.helioviewer.jhv.timelines.gui;

import javax.swing.JMenuItem;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.band.BandReaderHapi;
import org.helioviewer.jhv.timelines.chart.PlotPanel;

public final class TimelinesGUI implements Timelines.Listener {

    private final TimelineDialog dialog = new TimelineDialog();
    private final TimelinePanel panel = new TimelinePanel(dialog);
    private final PlotPanel plot = new PlotPanel();
    private final JMenuItem newItem = new JMenuItem(new TimelineActions.NewLayer(dialog));
    private final JMenuItem openItem = new JMenuItem(new TimelineActions.OpenLocalFile());

    public void install() {
        MainFrame.getLayersPanel().setTimelinesPanel(panel);
        MainFrame.getMainContentPanel().setTimelinesPanel(plot);
        MainFrame.getMenuBar().getMenu(0).add(newItem, 5);
        MainFrame.getMenuBar().getMenu(0).add(openItem, 6);
        Timelines.addListener(this);
    }

    public void uninstall() {
        Timelines.removeListener(this);
        MainFrame.getMenuBar().getMenu(0).remove(openItem);
        MainFrame.getMenuBar().getMenu(0).remove(newItem);
        MainFrame.getMainContentPanel().setTimelinesPanel(null);
        MainFrame.getLayersPanel().setTimelinesPanel(null);
    }

    @Override
    public void catalogLoaded(BandReaderHapi.CatalogData catalogData) {
        dialog.setCatalogs(catalogData.datasets());
        panel.setPredefinedGroups(catalogData.predefinedGroups());
    }

}
