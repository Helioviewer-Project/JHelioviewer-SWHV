package org.helioviewer.jhv.plugins.pfss;

import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.selector.LayerOptions;
import org.helioviewer.jhv.plugins.Plugin;

import org.json.JSONObject;

public class PfssPlugin extends Plugin {

    private static final PfssCache pfssCache = new PfssCache();
    private static final PfssLayer layer = new PfssLayer(null);

    public PfssPlugin() {
        super("PFSS", "Visualize PFSS model data");
    }

    static PfssCache getPfssCache() {
        return pfssCache;
    }

    @Override
    public void install() {
        LayerOptions.register(PfssLayer.class, layer -> new PfssLayerOptions((PfssLayer) layer));
        Layers.add(layer);
    }

    @Override
    public void uninstall() {
        Layers.remove(layer);
        LayerOptions.unregister(PfssLayer.class);
        pfssCache.clear();
    }

    @Override
    public void saveState(JSONObject jo) {}

    @Override
    public void loadState(JSONObject jo) {}

}
