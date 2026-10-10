package org.helioviewer.jhv.timelines.gui;

import java.util.IdentityHashMap;

import javax.annotation.Nullable;
import javax.swing.JPanel;

import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.radio.RadioData;

// Options panels per timeline layer, created on demand and dropped with the layer.
final class TimelineOptions implements TimelineLayers.Listener {

    private final IdentityHashMap<TimelineLayer, JPanel> panels = new IdentityHashMap<>();

    TimelineOptions() {
        TimelineLayers.addListener(this);
    }

    @Nullable
    JPanel getOptionsPanel(TimelineLayer layer) {
        JPanel panel = panels.get(layer);
        if (panel == null) {
            panel = createOptionsPanel(layer);
            if (panel != null)
                panels.put(layer, panel);
        }
        return panel;
    }

    @Nullable
    private static JPanel createOptionsPanel(TimelineLayer layer) {
        if (layer instanceof Band band)
            return new BandOptions(band);
        if (layer instanceof RadioData radio)
            return new RadioOptions(radio);
        return null;
    }

    @Override
    public void layerRemoved(int row, TimelineLayer layer) {
        panels.remove(layer);
    }

    @Override
    public void layersReplaced() {
        panels.keySet().retainAll(TimelineLayers.get());
    }

}
