package org.helioviewer.jhv.plugins.swek;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.selector.LayerOptions;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.timelines.Timelines;

import org.json.JSONObject;

public class SWEKPlugin extends Plugin {

    private static SWEKPlugin instance;
    private static SWEKLayer layer;
    private EventTimelineLayer etl;
    private SWEKTreePane swekPanel;
    private SWEKPopupController popupController;

    public SWEKPlugin() {
        super("Space Weather Event Knowledgebase", "Visualize space weather relevant events");
        instance = this;
    }

    @Override
    public void install() {
        layer = new SWEKLayer(null);
        layer.setEnabled(true);
        Layers.add(layer);
    }

    @Override
    public void uninstall() {
        Layers.remove(layer);
        layer = null;
    }

    @Override
    public void installGUI() {
        etl = new EventTimelineLayer();
        swekPanel = new SWEKTreePane(SWEKConfig.load());
        popupController = new SWEKPopupController();
        bindLayer();

        MainFrame.getLeftContentPane().add("Space Weather Event Knowledgebase", swekPanel, true);
        MainFrame.getLeftContentPane().revalidate();

        LayerOptions.register(SWEKLayer.class, layer -> new SWEKLayerOptionsPanel((SWEKLayer) layer));
        popupController.install();
        Timelines.getLayers().add(etl);
    }

    @Override
    public void uninstallGUI() {
        Timelines.getLayers().remove(etl);
        popupController.uninstall();
        popupController.setLayer(null);
        LayerOptions.unregister(SWEKLayer.class);

        MainFrame.getLeftContentPane().remove(swekPanel);
        MainFrame.getLeftContentPane().revalidate();
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

    static void restoreLayer(SWEKLayer _layer) {
        layer = _layer;
        instance.bindLayer();
    }

    static void layerStateChanged(SWEKLayer _layer) {
        if (layer == _layer)
            instance.bindLayer();
    }

    private void bindLayer() {
        if (popupController != null)
            popupController.setLayer(layer);
    }

}
