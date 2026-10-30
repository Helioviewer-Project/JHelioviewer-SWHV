package org.helioviewer.jhv.layers;

import javax.annotation.Nullable;

import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;

import org.json.JSONObject;

public interface Layer {

    default void render(MapView mv, Viewport vp) {}

    default void renderScale(MapView mv, Viewport vp) {}

    default void renderFloat(MapView mv, Viewport vp) {}

    default void renderFullFloat(Viewport vp) {}

    // Stops CPU work without requiring a GL context.
    default void detach() {
        setEnabled(false);
    }

    String getName();

    boolean isEnabled();

    void setEnabled(boolean b);

    @Nullable
    default String getTimeString() {
        return null;
    }

    default boolean isDeletable() {
        return false;
    }

    default boolean isDownloading() {
        return false;
    }

    default boolean isLocal() {
        return false;
    }

    void init();

    // Releases GL resources, including during context recreation.
    void dispose();

    void serialize(JSONObject jo);

}
