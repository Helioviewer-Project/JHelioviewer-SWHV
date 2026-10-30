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
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MiniviewLayer;
import org.helioviewer.jhv.layers.TimestampLayer;
import org.helioviewer.jhv.layers.fov.FOVTreePane;

// Options panels per layer, created on demand and dropped with the layer. Plugins register providers.
public final class LayerOptions implements Layers.Listener {

    private static final Map<Class<? extends Layer>, Function<Layer, Component>> providers = new HashMap<>();
    private final IdentityHashMap<Layer, Component> panels = new IdentityHashMap<>();

    static {
        register(ConnectionLayer.class, ConnectionLayerOptions::new);
        register(FOVLayer.class, layer -> new FOVTreePane(layer.getCatalog()));
        register(GridLayer.class, GridLayerOptions::new);
        register(ImageLayer.class, ImageLayerOptions::new);
        register(MiniviewLayer.class, MiniviewLayerOptions::new);
        register(TimestampLayer.class, TimestampLayerOptions::new);
    }

    LayerOptions() {
        Layers.addListener(this);
    }

    public static <T extends Layer> void register(Class<T> type, Function<T, Component> provider) {
        providers.put(type, layer -> provider.apply(type.cast(layer)));
    }

    public static void unregister(Class<? extends Layer> type) {
        providers.remove(type);
    }

    @Nullable
    Component getOptionsPanel(Layer layer) {
        return panels.computeIfAbsent(layer, LayerOptions::createOptionsPanel);
    }

    @Nullable
    private static Component createOptionsPanel(Layer layer) {
        Function<Layer, Component> provider = providers.get(layer.getClass());
        return provider == null ? null : provider.apply(layer);
    }

    @Override
    public void layerRemoved(int index, Layer layer) {
        panels.remove(layer);
    }

    @Override
    public void layersCleared() {
        panels.clear();
    }

    @Override
    public void layerUpdated(Layer layer) {
        if (layer instanceof ImageLayer imageLayer && panels.get(layer) instanceof ImageLayerOptions panel)
            panel.refresh(imageLayer);
    }

}
