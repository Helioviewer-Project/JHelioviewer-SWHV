package org.helioviewer.jhv.plugins.swek;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.timelines.Timelines;

import org.json.JSONObject;

public class SWEKPlugin extends Plugin {

    private static final SWEKLayer layer = new SWEKLayer(null);
    private static final EventTimelineLayer etl = new EventTimelineLayer();
    private final SWEKTreePane swekPanel = new SWEKTreePane(SWEKConfig.load());

    public SWEKPlugin() {
        super("Space Weather Event Knowledgebase", "Visualize space weather relevant events");
    }

    @Override
    public void install() {
        JHVFrame.getLeftContentPane().add("Space Weather Event Knowledgebase", swekPanel, true);
        JHVFrame.getLeftContentPane().revalidate();
        Layers.add(layer);
        Timelines.getLayers().add(etl);
    }

    @Override
    public void uninstall() {
        JHVFrame.getLeftContentPane().remove(swekPanel);
        JHVFrame.getLeftContentPane().revalidate();
        Layers.remove(layer);
        Timelines.getLayers().remove(etl);
    }

    @Override
    public void saveState(JSONObject jo) {
        // functionality to be restored later
    }

    @Override
    public void loadState(JSONObject jo) {
        // functionality to be restored later
    }

}
