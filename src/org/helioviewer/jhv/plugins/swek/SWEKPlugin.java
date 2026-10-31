package org.helioviewer.jhv.plugins.swek;

import java.util.Map;

import javax.annotation.Nullable;

import org.helioviewer.jhv.event.SWEKConfig;
import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.sources.HEKHandler;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.plugins.swek.swing.SWEKSwingUI;
import org.helioviewer.jhv.timelines.TimelineLayers;

import org.json.JSONObject;

public class SWEKPlugin extends Plugin implements Layers.Listener {

    // The toolkit side of the plugin, for one activation.
    public interface Ui extends SWEKLayer.CanvasUi, EventTimelineLayer.Ui {
        void close();
    }

    private SWEKLayer layer;
    private EventTimelineLayer etl;
    private SWEKSession session;
    private Ui ui;

    public SWEKPlugin() {
        super("events", "SWEKPlugin", "Events", "Visualize space weather relevant events");
    }

    @Override
    protected void install() {
        session = new SWEKSession(SWEKConfig.load(Map.of("HEK", new HEKHandler())));
        layer = new SWEKLayer(session);
        layer.setEnabled(true);
        Layers.add(layer);
        Layers.addListener(this);
        etl = new EventTimelineLayer(session);
        TimelineLayers.add(etl);
    }

    @Override
    protected void uninstall() {
        Layers.removeListener(this);
        TimelineLayers.remove(etl);
        etl = null;
        if (layer != null) {
            layer.detach();
            Layers.remove(layer);
        }
        layer = null;
        if (session != null)
            session.close();
        session = null;
    }

    @Override
    protected void installGUI() {
        SWEKSwingUI swing = new SWEKSwingUI(session);
        swing.install();
        attachUi(swing);
    }

    @Override
    protected void uninstallGUI() {
        detachUi();
    }

    private void attachUi(Ui _ui) {
        ui = _ui;
        etl.setUi(ui);
        bindLayer();
    }

    private void detachUi() {
        if (etl != null)
            etl.setUi(null);
        if (ui != null)
            ui.close();
        ui = null;
        bindLayer();
    }

    public SWEKLayer restoreLayer(JSONObject data) {
        if (session == null)
            throw new IllegalStateException("SWEK plugin is inactive");
        SWEKLayer restored = new SWEKLayer(session, data);
        if (layer != null)
            layer.detach();
        layer = restored;
        bindLayer();
        return restored;
    }

    @Nullable
    public EventTimelineLayer getTimelineLayer() {
        return etl;
    }

    @Override
    public void layersCleared() {
        if (layer != null && !Layers.getLayers().contains(layer))
            layer = null;
    }

    private void bindLayer() {
        if (layer != null)
            layer.setUi(ui);
    }

}
