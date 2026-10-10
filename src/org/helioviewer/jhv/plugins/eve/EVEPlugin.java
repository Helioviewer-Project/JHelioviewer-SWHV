package org.helioviewer.jhv.plugins.eve;

import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.gui.TimelinesGUI;

import org.json.JSONObject;

public class EVEPlugin extends Plugin {

    private TimelinesGUI gui;

    public EVEPlugin() {
        super("Timelines", "Visualize 1D and 2D time series");
    }

    @Override
    public void install() {
        Timelines.install();
    }

    @Override
    public void uninstall() {
        Timelines.uninstall();
    }

    @Override
    public void installGUI() {
        if (gui == null)
            gui = new TimelinesGUI();
        gui.install();
    }

    @Override
    public void uninstallGUI() {
        gui.uninstall();
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
