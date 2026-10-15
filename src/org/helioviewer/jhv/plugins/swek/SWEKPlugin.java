package org.helioviewer.jhv.plugins.swek;

import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKDownloader;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.filter.FilterManager;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.selector.LayerOptions;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.timelines.TimelineLayers;

import org.json.JSONObject;

public class SWEKPlugin extends Plugin {

    private static SWEKPlugin instance;
    private static SWEKLayer layer;
    private EventTimelineLayer etl;
    private List<SWEKGroup> groups = List.of();
    private SWEKTreePane swekPanel;
    private SWEKPopupController popupController;

    public SWEKPlugin() {
        super("Space Weather Event Knowledgebase", "Visualize space weather relevant events");
        instance = this;
    }

    @Override
    public void install() {
        groups = SWEKConfig.load();
        layer = new SWEKLayer(null);
        layer.setEnabled(true);
        Layers.add(layer);
        SWEKDownloader.installFilterListener();
        etl = new EventTimelineLayer();
        TimelineLayers.add(etl);
    }

    @Override
    public void uninstall() {
        TimelineLayers.remove(etl);
        etl = null;
        SWEKDownloader.removeFilterListener();
        layer.setEnabled(false);
        Layers.remove(layer);
        layer = null;
        SWEKDownloader.clear();
        FilterManager.clear();
        SWEKCatalog.clear();
        EventDatabase.clearStoredIntervals();
        groups = List.of();
    }

    @Override
    public void installGUI() {
        swekPanel = new SWEKTreePane(groups);
        popupController = new SWEKPopupController();
        bindLayer();

        MainFrame.getLeftContentPane().add("Space Weather Event Knowledgebase", swekPanel, true);
        MainFrame.getLeftContentPane().revalidate();

        LayerOptions.register(SWEKLayer.class, layer -> new SWEKLayerOptionsPanel((SWEKLayer) layer));
        popupController.install();
    }

    @Override
    public void uninstallGUI() {
        popupController.uninstall();
        popupController.setLayer(null);
        LayerOptions.unregister(SWEKLayer.class);

        MainFrame.getLeftContentPane().remove(swekPanel);
        MainFrame.getLeftContentPane().revalidate();
        swekPanel = null;
        popupController = null;
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

    @Nullable
    static EventTimelineLayer getTimelineLayer() {
        return instance == null ? null : instance.etl;
    }

    private void bindLayer() {
        if (popupController != null)
            popupController.setLayer(layer);
    }

}
