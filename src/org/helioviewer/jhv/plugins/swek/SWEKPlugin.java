package org.helioviewer.jhv.plugins.swek;

import java.util.Map;

import javax.annotation.Nullable;

import org.helioviewer.jhv.event.SWEKConfig;
import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.sources.HEKHandler;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.selector.LayerOptions;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.timelines.TimelineLayers;

import org.json.JSONObject;

public class SWEKPlugin extends Plugin implements Layers.Listener {

    private static SWEKPlugin instance;
    private SWEKLayer layer;
    private EventTimelineLayer etl;
    private SWEKSession session;
    private SWEKDialogs dialogs;
    private SWEKTreePane swekPanel;
    private SWEKPopupController popupController;

    public SWEKPlugin() {
        super("Space Weather Event Knowledgebase", "Visualize space weather relevant events");
        instance = this;
    }

    @Override
    public void install() {
        session = new SWEKSession(SWEKConfig.load(Map.of("HEK", new HEKHandler())));
        layer = new SWEKLayer(session);
        layer.setEnabled(true);
        Layers.add(layer);
        Layers.addListener(this);
        etl = new EventTimelineLayer(session);
        TimelineLayers.add(etl);
    }

    @Override
    public void uninstall() {
        Layers.removeListener(this);
        TimelineLayers.remove(etl);
        etl = null;
        if (layer != null) {
            layer.detach();
            Layers.remove(layer);
        }
        layer = null;
        session.close();
        session = null;
    }

    @Override
    public void installGUI() {
        dialogs = new SWEKDialogs(session);
        etl.setDialogs(dialogs);
        swekPanel = new SWEKTreePane(session, dialogs);
        popupController = new SWEKPopupController(dialogs);
        bindLayer();

        MainFrame.getLeftContentPane().add("Space Weather Event Knowledgebase", swekPanel, true);
        MainFrame.getLeftContentPane().revalidate();

        LayerOptions.register(SWEKLayer.class, selectedLayer -> new SWEKLayerOptionsPanel((SWEKLayer) selectedLayer));
    }

    @Override
    public void uninstallGUI() {
        popupController = null;
        bindLayer();
        LayerOptions.unregister(SWEKLayer.class);
        etl.setDialogs(null);
        dialogs.close();
        dialogs = null;

        MainFrame.getLeftContentPane().remove(swekPanel);
        MainFrame.getLeftContentPane().revalidate();
        swekPanel = null;
    }

    @Override
    public void saveState(JSONObject jo) {
        // functionality to be restored later
    }

    @Override
    public void loadState(JSONObject jo) {
        // functionality to be restored later
    }

    static SWEKSession sessionForRestore() {
        if (instance == null || instance.session == null)
            throw new IllegalStateException("SWEK plugin is inactive");
        return instance.session;
    }

    static void restoreLayer(SWEKLayer restored) {
        if (instance.layer != null)
            instance.layer.detach();
        instance.layer = restored;
        instance.bindLayer();
    }

    @Nullable
    static EventTimelineLayer getTimelineLayer() {
        return instance == null ? null : instance.etl;
    }

    @Override
    public void layerRemoved(int index, Layer removed) {
        if (removed == layer)
            detachLayer();
    }

    @Override
    public void layersCleared() {
        if (layer != null && !Layers.getLayers().contains(layer))
            detachLayer();
    }

    private void detachLayer() {
        layer.detach();
        layer = null;
    }

    private void bindLayer() {
        if (layer != null)
            layer.setUi(popupController);
    }

}
