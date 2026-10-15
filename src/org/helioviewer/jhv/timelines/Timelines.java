package org.helioviewer.jhv.timelines;

import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.time.TimeListener;
import org.helioviewer.jhv.timelines.band.BandReaderHapi;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.radio.RadioData;

import org.json.JSONObject;

// Timelines model: layers, draw controller and the HAPI catalog, without any UI.
public final class Timelines {

    public interface Listener {
        void catalogLoaded(BandReaderHapi.CatalogData catalogData);
    }

    private static final TimeListener.Change timeListener = DrawController::timeChanged;
    private static final ArrayList<Listener> listeners = new ArrayList<>();
    @Nullable
    private static BandReaderHapi.CatalogData catalog;

    public static void install() {
        if (TimelineLayers.get().stream().noneMatch(layer -> layer instanceof RadioData))
            TimelineLayers.add(new RadioData(null));
        Player.addTimeListener(timeListener);
        requestCatalog();
    }

    public static void uninstall() {
        Player.removeTimeListener(timeListener);
    }

    // Replays the last catalog to a late listener.
    public static void addListener(Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            if (catalog != null)
                listener.catalogLoaded(catalog);
        }
    }

    public static void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public static void requestCatalog() {
        BandReaderHapi.requestCatalog(Timelines::catalogLoaded);
    }

    private static void catalogLoaded(BandReaderHapi.CatalogData catalogData) {
        catalog = catalogData;
        TimelineLayers.fetchBands();
        listeners.forEach(listener -> listener.catalogLoaded(catalogData));
    }

    public static void saveState(JSONObject jo) {
        DrawController.saveState(jo);
    }

    public static void loadState(JSONObject jo) {
        DrawController.loadState(jo);
    }

    private Timelines() {}

}
