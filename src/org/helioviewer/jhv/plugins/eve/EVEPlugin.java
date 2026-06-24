package org.helioviewer.jhv.plugins.eve;

import javax.swing.JMenuItem;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.band.BandReaderHapi;
import org.helioviewer.jhv.timelines.gui.TimelineActions;

import org.json.JSONObject;

public class EVEPlugin extends Plugin {

    private Timelines tl;
    private JMenuItem newItem;
    private JMenuItem openItem;

    public EVEPlugin() {
        super("Timelines", "Visualize 1D and 2D time series");
    }

    @Override
    public void install() {}

    @Override
    public void uninstall() {}

    @Override
    public void installGUI() {
        tl = new Timelines();
        tl.installTimelines();
        newItem = new JMenuItem(new TimelineActions.NewLayer());
        openItem = new JMenuItem(new TimelineActions.OpenLocalFile());
        MainFrame.getMenuBar().getMenu(0).add(newItem, 5);
        MainFrame.getMenuBar().getMenu(0).add(openItem, 6);

        BandReaderHapi.requestCatalog();
    }

    @Override
    public void uninstallGUI() {
        MainFrame.getMenuBar().getMenu(0).remove(openItem);
        MainFrame.getMenuBar().getMenu(0).remove(newItem);
        openItem = null;
        newItem = null;
        tl.uninstallTimelines();
        tl = null;
    }

    @Override
    public void saveState(JSONObject jo) {
        Timelines.saveState(jo);
    }

    @Override
    public void loadState(JSONObject jo) {
        Timelines.loadState(jo);
    }

}
