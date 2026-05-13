package org.helioviewer.jhv.layers.selector;

import java.awt.Component;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;

final class LayerOptions implements Layers.Listener {

    interface OptionsPanel {
        default void refresh(Layer layer) {}
    }

    private static final Map<Class<? extends Layer>, Function<Layer, Component>> providers = new HashMap<>();
    private static final Map<Layer, Component> panels = new IdentityHashMap<>();
    private static final LayerOptions listener = new LayerOptions();

    static {
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
        if (panel instanceof OptionsPanel optionsPanel)
            optionsPanel.refresh(layer);
        return panel;
    }

    @Nullable
    private static Component createOptionsPanel(Layer layer) {
        Function<Layer, Component> provider = providers.get(layer.getClass());
        return provider == null ? layer.getOptionsPanel() : provider.apply(layer);
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
    public void layerUpdated(Layer layer) {}

    @Override
    public void timeUpdated(Layer layer) {}

    private LayerOptions() {}

}
