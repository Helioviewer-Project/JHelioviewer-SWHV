package org.helioviewer.jhv.timelines.gui;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.swing.JPanel;

import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.radio.RadioData;

// Options panels per timeline layer class; layers without a provider have none.
final class TimelineOptions implements TimelineLayers.Listener {

    private static final Map<Class<? extends TimelineLayer>, Function<TimelineLayer, JPanel>> providers = new HashMap<>();
    private static final IdentityHashMap<TimelineLayer, JPanel> panels = new IdentityHashMap<>();

    static {
        providers.put(Band.class, layer -> new BandOptions((Band) layer));
        providers.put(RadioData.class, layer -> new RadioOptions((RadioData) layer));
        TimelineLayers.addListener(new TimelineOptions());
    }

    @Nullable
    static JPanel getOptionsPanel(TimelineLayer layer) {
        JPanel panel = panels.get(layer);
        if (panel == null) {
            Function<TimelineLayer, JPanel> provider = providers.get(layer.getClass());
            if (provider != null) {
                panel = provider.apply(layer);
                panels.put(layer, panel);
            }
        }
        return panel;
    }

    @Override
    public void layerRemoved(int row, TimelineLayer layer) {
        panels.remove(layer);
    }

    @Override
    public void layersReplaced() {
        panels.keySet().retainAll(TimelineLayers.get());
    }

    private TimelineOptions() {}

}
