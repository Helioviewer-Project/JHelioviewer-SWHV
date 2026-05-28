package org.helioviewer.jhv.plugins.swek;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.selector.LayerOptions;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.timelines.Timelines;

import org.json.JSONObject;

public class SWEKPlugin extends Plugin {

    private static final SWEKLayer layer = new SWEKLayer(null);
    private EventTimelineLayer etl;
    private SWEKTreePane swekPanel;
    private SWEKPopupController popupController;

    public SWEKPlugin() {
        super("Space Weather Event Knowledgebase", "Visualize space weather relevant events");
    }

    @Override
    public void install() {
        layer.setEnabled(true);
        Layers.add(layer);
    }

    @Override
    public void uninstall() {
        Layers.remove(layer);
    }

    @Override
    public void installGUI() {
        etl = new EventTimelineLayer();
        swekPanel = new SWEKTreePane(SWEKConfig.load());
        popupController = new SWEKPopupController();
        layer.setContext(popupController.context());

        JHVFrame.getLeftContentPane().add("Space Weather Event Knowledgebase", swekPanel, true);
        JHVFrame.getLeftContentPane().revalidate();

        LayerOptions.register(SWEKLayer.class, layer -> new SWEKLayerOptionsPanel((SWEKLayer) layer));
        popupController.install();
        Timelines.getLayers().add(etl);
    }

    @Override
    public void uninstallGUI() {
        Timelines.getLayers().remove(etl);
        popupController.uninstall();
        LayerOptions.unregister(SWEKLayer.class);

        JHVFrame.getLeftContentPane().remove(swekPanel);
        JHVFrame.getLeftContentPane().revalidate();
        swekPanel = null;
        popupController = null;
        etl = null;
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
