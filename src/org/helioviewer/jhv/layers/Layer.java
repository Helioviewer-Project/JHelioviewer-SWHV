package org.helioviewer.jhv.layers;

import javax.annotation.Nullable;

import org.helioviewer.jhv.display.MapContext;
import org.helioviewer.jhv.display.ProjectionScale;
import org.helioviewer.jhv.display.Viewport;

import org.json.JSONObject;

public interface Layer {

    default void render(MapContext ctx, Viewport vp, ProjectionScale scale) {}

    default void renderScale(MapContext ctx, Viewport vp, ProjectionScale scale) {}

    default void renderFloat(MapContext ctx, Viewport vp, ProjectionScale scale) {}

    default void renderFullFloat(MapContext ctx, Viewport vp) {}

    default void renderMiniview(MapContext ctx, Viewport vp, ProjectionScale scale) {}

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
