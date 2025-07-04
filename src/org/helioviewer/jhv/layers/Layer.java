package org.helioviewer.jhv.layers;

import java.awt.Component;

import javax.annotation.Nullable;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.json.JSONObject;

import com.jogamp.opengl.GL3;

public interface Layer {

    default void render(Camera camera, Viewport vp, GL3 gl) {
    }

    default void renderScale(Camera camera, Viewport vp, GL3 gl) {
    }

    default void renderFloat(Camera camera, Viewport vp, GL3 gl) {
    }

    default void renderFullFloat(Camera camera, Viewport vp, GL3 gl) {
    }

    default void renderMiniview(Camera camera, Viewport vp, GL3 gl) {
    }

    default void prerender(GL3 gl) {
    }

    void remove(GL3 gl);

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

    void init(GL3 gl);

    void dispose(GL3 gl);

    void serialize(JSONObject jo);

}
