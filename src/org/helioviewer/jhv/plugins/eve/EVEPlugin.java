package org.helioviewer.jhv.plugins.eve;

import javax.swing.JMenuItem;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.band.HapiReader;
import org.helioviewer.jhv.timelines.gui.TimelineActions;
import org.json.JSONObject;

public class EVEPlugin extends Plugin {

    private final Timelines tl = new Timelines();
    private final JMenuItem newItem = new JMenuItem(new TimelineActions.NewLayer());
    private final JMenuItem openItem = new JMenuItem(new TimelineActions.OpenLocalFile());

    public EVEPlugin() {
        super("Timelines", "Visualize 1D and 2D time series");
    }

    @Override
    public void install() {
        tl.installTimelines();
        HapiReader.requestCatalog();
        JHVFrame.getMenuBar().getMenu(0).add(newItem, 2);
        JHVFrame.getMenuBar().getMenu(0).add(openItem, 4);
    }

    @Override
    public void uninstall() {
        tl.uninstallTimelines();
        JHVFrame.getMenuBar().getMenu(0).remove(newItem);
        JHVFrame.getMenuBar().getMenu(0).remove(openItem);
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
