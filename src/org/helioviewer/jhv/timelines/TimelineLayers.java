package org.helioviewer.jhv.timelines;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.band.BandType;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.radio.RadioData;

public final class TimelineLayers {

    public interface Listener {
        default void layersAdded(int firstRow, int lastRow) {}

        default void layerRemoved(int row, TimelineLayer layer) {}

        default void layersReplaced() {}

        default void layersUpdated(int firstRow, int lastRow) {}
    }

    private static final ArrayList<TimelineLayer> layers = new ArrayList<>();
    private static final List<TimelineLayer> extLayers = Collections.unmodifiableList(layers);
    private static final ArrayList<Listener> listeners = new ArrayList<>();

    public static List<TimelineLayer> get() {
        return extLayers;
    }

    public static void addListener(Listener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public static void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public static void fetchData(TimeAxis timeAxis) {
        for (TimelineLayer layer : layers) {
            if (layer.isEnabled())
                layer.fetchData(timeAxis);
        }
    }

    static void fetchBands() {
        for (TimelineLayer layer : layers) {
            if (layer.isEnabled() && layer instanceof Band band)
                band.fetchData(DrawController.selectedAxis);
        }
    }

    private static void updateRow(TimelineLayer layer) {
        int row = layers.indexOf(layer);
        if (row >= 0)
            listeners.forEach(listener -> listener.layersUpdated(row, row));
    }

    public static void updateRows(Collection<? extends TimelineLayer> changedLayers) {
        int firstRow = layers.size();
        int lastRow = -1;
        for (TimelineLayer layer : changedLayers) {
            int row = layers.indexOf(layer);
            if (row >= 0) {
                firstRow = Math.min(firstRow, row);
                lastRow = Math.max(lastRow, row);
            }
        }
        if (lastRow >= 0) {
            int first = firstRow;
            int last = lastRow;
            listeners.forEach(listener -> listener.layersUpdated(first, last));
        }
    }

    public static List<Band> addBands(List<BandType> bandTypes) {
        return addBands(bandTypes, false);
    }

    public static List<Band> addBands(List<BandType> bandTypes, boolean fullResolution) {
        int firstRow = layers.size();
        ArrayList<Band> bands = new ArrayList<>(bandTypes.size());
        ArrayList<Band> added = new ArrayList<>();
        for (BandType bandType : bandTypes) {
            Band band = findBand(layers, bandType);
            if (band == null) {
                band = new Band(bandType, fullResolution);
                layers.add(band);
                configureLayer(band);
                added.add(band);
            }
            bands.add(band);
        }
        if (!added.isEmpty()) {
            int lastRow = layers.size() - 1;
            listeners.forEach(listener -> listener.layersAdded(firstRow, lastRow));
            DrawController.layoutChanged();
            added.forEach(layer -> layer.fetchData(DrawController.selectedAxis));
        }
        return bands;
    }

    public static void add(TimelineLayer tl) {
        if (containsLayer(layers, tl))
            return;
        layers.add(tl);

        configureLayer(tl);

        int row = layers.size() - 1;
        listeners.forEach(listener -> listener.layersAdded(row, row));
        DrawController.layoutChanged();
        tl.fetchData(DrawController.selectedAxis);
    }

    public static void remove(TimelineLayer tl) {
        int row = layers.indexOf(tl);
        if (row == -1)
            return;

        tl.remove();
        layers.remove(row);
        listeners.forEach(listener -> listener.layerRemoved(row, tl));
        DrawController.layoutChanged();
    }

    public static void restore(List<TimelineLayer> newLayers) {
        ArrayList<TimelineLayer> restoredLayers = new ArrayList<>();
        for (TimelineLayer newLayer : newLayers) {
            TimelineLayer layer = reuseBand(newLayer);
            addUnique(restoredLayers, layer);
        }
        // Band and Callisto rows come from the state; plugin rows belong to their active plugin.
        for (TimelineLayer layer : layers) {
            if (!(layer instanceof Band) && !(layer instanceof RadioData))
                addUnique(restoredLayers, layer);
        }
        if (restoredLayers.stream().noneMatch(layer -> layer instanceof RadioData))
            restoredLayers.add(new RadioData(null));
        replaceAll(restoredLayers);
    }

    private static TimelineLayer reuseBand(TimelineLayer restored) {
        if (restored instanceof Band restoredBand) {
            Band band = findBand(layers, restoredBand.getBandType());
            if (band != null && band.isFullResolution() == restoredBand.isFullResolution()) {
                band.applyStateFrom(restoredBand);
                return band;
            }
        }
        return restored;
    }

    public static void replaceBands(List<BandType> bandTypes) {
        ArrayList<TimelineLayer> replacement = new ArrayList<>();
        HashMap<BandType, Band> existingBands = new HashMap<>();
        for (TimelineLayer layer : layers) {
            if (layer instanceof Band band)
                existingBands.putIfAbsent(band.getBandType(), band);
            else
                replacement.add(layer);
        }

        bandTypes.stream()
                .distinct()
                .map(type -> existingBands.computeIfAbsent(type, Band::new))
                .forEach(replacement::add);
        replaceAll(replacement);
    }

    private static void replaceAll(List<TimelineLayer> replacement) {
        HashSet<TimelineLayer> retained = new HashSet<>(replacement);
        for (TimelineLayer layer : layers) {
            if (!retained.contains(layer))
                layer.remove();
        }
        layers.clear();
        layers.addAll(replacement);
        replacement.forEach(TimelineLayers::configureLayer);
        listeners.forEach(Listener::layersReplaced);
        DrawController.layoutChanged();
        fetchData(DrawController.selectedAxis);
    }

    private static void configureLayer(TimelineLayer layer) {
        layer.setOnStateChanged(() -> updateRow(layer));
    }

    private static void addUnique(List<TimelineLayer> target, TimelineLayer layer) {
        if (!containsLayer(target, layer))
            target.add(layer);
    }

    private static boolean containsLayer(List<TimelineLayer> searchLayers, TimelineLayer target) {
        if (target instanceof Band band)
            return findBand(searchLayers, band.getBandType()) != null;
        return searchLayers.contains(target);
    }

    @Nullable
    private static Band findBand(List<TimelineLayer> searchLayers, BandType bandType) {
        for (TimelineLayer layer : searchLayers) {
            if (layer instanceof Band band && band.getBandType().equals(bandType))
                return band;
        }
        return null;
    }

    private TimelineLayers() {}

}
