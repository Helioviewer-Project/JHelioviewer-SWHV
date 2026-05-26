package org.helioviewer.jhv.layers;

import javax.annotation.Nullable;

import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.Viewport;

import org.json.JSONObject;

public interface Layer {

    default void render(MapView ctx, Viewport vp, MapScale scale) {}

    default void renderScale(MapView ctx, Viewport vp, MapScale scale) {}

    default void renderFloat(MapView ctx, Viewport vp, MapScale scale) {}

    default void renderFullFloat(Viewport vp) {}

    default void renderMiniview(MapView ctx, Viewport vp, MapScale scale) {}

    default void prerender() {}

    void remove();

    String getName();

    boolean isEnabled();

    void setEnabled(boolean b);

    int isVisibleIdx();

    boolean isVisible(int idx);

    void setVisible(int idx);

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

    void dispose();

    void serialize(JSONObject jo);

}
