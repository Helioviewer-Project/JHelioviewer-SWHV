package org.helioviewer.jhv.layers.selector;

import java.awt.Component;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.helioviewer.jhv.layers.ConnectionLayer;
import org.helioviewer.jhv.layers.FOVLayer;
import org.helioviewer.jhv.layers.GridLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MiniviewLayer;
import org.helioviewer.jhv.layers.TimestampLayer;
import org.helioviewer.jhv.layers.ViewpointLayer;
import org.helioviewer.jhv.layers.fov.FOVTreePane;

public final class LayerOptions implements Layers.Listener {

    private static final Map<Class<? extends Layer>, Function<Layer, Component>> providers = new HashMap<>();
    private static final Map<Layer, Component> panels = new IdentityHashMap<>();
    private static final LayerOptions listener = new LayerOptions();

    static {
        register(ConnectionLayer.class, layer -> new ConnectionLayerOptions((ConnectionLayer) layer));
        register(FOVLayer.class, layer -> new FOVTreePane(((FOVLayer) layer).getCatalog()));
        register(GridLayer.class, layer -> new GridLayerOptions((GridLayer) layer));
        register(MiniviewLayer.class, layer -> new MiniviewLayerOptions((MiniviewLayer) layer));
        register(TimestampLayer.class, layer -> new TimestampLayerOptions((TimestampLayer) layer));
        register(ViewpointLayer.class, layer -> new ViewpointLayerOptionsPanel((ViewpointLayer) layer));
        Layers.addListener(listener);
    }

    public static void register(Class<? extends Layer> type, Function<Layer, Component> provider) {
        providers.put(type, provider);
    }

    public static void unregister(Class<? extends Layer> type) {
        providers.remove(type);
    }

    @Nullable
    public static Component getOptionsPanel(Layer layer) {
        Component panel = panels.get(layer);
        if (panel == null) {
            panel = createOptionsPanel(layer);
            if (panel != null)
                panels.put(layer, panel);
        }
        return panel;
    }

    @Nullable
    private static Component createOptionsPanel(Layer layer) {
        Function<Layer, Component> provider = providers.get(layer.getClass());
        return provider == null ? null : provider.apply(layer);
    }

    @Override
    public void layerAdded(int index, Layer layer) {}

    @Override
    public void layerRemoved(int index, Layer layer) {
        panels.remove(layer);
    }

    @Override
    public void layersCleared() {
        panels.clear();
    }

    @Override
    public void nameUpdated(Layer layer) {}

    @Override
    public void layerUpdated(Layer layer) {}

    @Override
    public void timeUpdated(Layer layer) {}

    private LayerOptions() {}
}
