package org.helioviewer.jhv.plugins.timelines;

import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.gui.TimelinesGUI;

import org.json.JSONObject;

public class TimelinesPlugin extends Plugin {

    private TimelinesGUI gui;

    public TimelinesPlugin() {
        super("timelines", "EVEPlugin", "Timelines", "Visualize 1D and 2D time series");
    }

    @Override
    protected void install() {
        Timelines.install();
    }

    @Override
    protected void uninstall() {
        Timelines.uninstall();
    }

    @Override
    protected void installGUI() {
        if (gui == null)
            gui = new TimelinesGUI();
        gui.install();
    }

    @Override
    protected void uninstallGUI() {
        if (gui != null)
            gui.uninstall();
    }

    @Override
    protected void writeState(JSONObject jo) {
        Timelines.saveState(jo);
    }

    @Override
    protected void readState(JSONObject jo) {
        Timelines.loadState(jo);
    }

}
