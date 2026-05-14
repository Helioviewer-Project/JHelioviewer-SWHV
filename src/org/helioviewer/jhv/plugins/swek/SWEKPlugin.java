package org.helioviewer.jhv.plugins.swek;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.selector.LayerOptions;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.timelines.Timelines;

import org.json.JSONObject;

public class SWEKPlugin extends Plugin {

    private static final SWEKLayer layer = new SWEKLayer(null);
    private static final EventTimelineLayer etl = new EventTimelineLayer();
    private final SWEKTreePane swekPanel = new SWEKTreePane(SWEKConfig.load());
    private final SWEKPopupController popupController = new SWEKPopupController(layer.getContext());

    public SWEKPlugin() {
        super("Space Weather Event Knowledgebase", "Visualize space weather relevant events");
    }

    @Override
    public void install() {
        JHVFrame.getLeftContentPane().add("Space Weather Event Knowledgebase", swekPanel, true);
        JHVFrame.getLeftContentPane().revalidate();

        LayerOptions.register(SWEKLayer.class, layer -> new SWEKLayerOptionsPanel((SWEKLayer) layer));
        layer.setEnabled(true);
        popupController.install();
        Layers.add(layer);
        Timelines.getLayers().add(etl);
    }

    @Override
    public void uninstall() {
        Timelines.getLayers().remove(etl);
        Layers.remove(layer);
        popupController.uninstall();
        LayerOptions.unregister(SWEKLayer.class);

        JHVFrame.getLeftContentPane().remove(swekPanel);
        JHVFrame.getLeftContentPane().revalidate();
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
