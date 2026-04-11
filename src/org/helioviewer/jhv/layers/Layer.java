package org.helioviewer.jhv.layers;

import java.awt.Component;

import javax.annotation.Nullable;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;

import org.json.JSONObject;

public interface Layer {

    default void render(Camera camera, Viewport vp) {
    }

    default void renderScale(Camera camera, Viewport vp) {
    }

    default void renderFloat(Camera camera, Viewport vp) {
    }

    default void renderFullFloat(Camera camera, Viewport vp) {
    }

    default void renderMiniview(Camera camera, Viewport vp) {
    }

    default void prerender() {
    }

    void remove();

    Component getOptionsPanel();

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
