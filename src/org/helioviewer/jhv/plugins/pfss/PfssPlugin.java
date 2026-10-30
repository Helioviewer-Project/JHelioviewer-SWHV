package org.helioviewer.jhv.plugins.pfss;

import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.selector.LayerOptions;
import org.helioviewer.jhv.plugins.Plugin;

import org.json.JSONObject;

public class PfssPlugin extends Plugin {

    private static final PfssCache pfssCache = new PfssCache();
    private PfssLayer layer = new PfssLayer(null);

    public PfssPlugin() {
        super("pfss", "PfssPlugin", "PFSS", "Visualize PFSS model data");
    }

    static PfssCache getPfssCache() {
        return pfssCache;
    }

    public PfssLayer restoreLayer(JSONObject data) {
        PfssLayer restored = new PfssLayer(data);
        layer.setEnabled(false);
        layer = restored;
        return restored;
    }

    @Override
    protected void install() {
        if (!Layers.getLayers().contains(layer))
            Layers.add(layer);
    }

    @Override
    protected void uninstall() {
        layer.setEnabled(false);
        Layers.remove(layer);
        pfssCache.clear();
    }

    @Override
    protected void installGUI() {
        LayerOptions.register(PfssLayer.class, layer -> new PfssLayerOptions((PfssLayer) layer));
    }

    @Override
    protected void uninstallGUI() {
        LayerOptions.unregister(PfssLayer.class);
    }

}
