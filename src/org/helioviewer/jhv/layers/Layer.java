package org.helioviewer.jhv.layers;

import java.awt.Component;

import javax.annotation.Nullable;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public interface Layer {

    void render(Camera camera, Viewport vp, GL2 gl);

    void renderScale(Camera camera, Viewport vp, GL2 gl);

    void renderFloat(Camera camera, Viewport vp, GL2 gl);

    void renderFullFloat(Camera camera, Viewport vp, GL2 gl);

    void renderMiniview(Camera camera, Viewport vp, GL2 gl);

    void prerender(GL2 gl);

    void remove(GL2 gl);

    Component getOptionsPanel();

    String getName();

    boolean isEnabled();

    void setEnabled(boolean b);

    int isVisibleIdx();

    boolean isVisible(int idx);

    void setVisible(int idx);

    @Nullable
    String getTimeString();

    boolean isDeletable();

    boolean isDownloading();

    boolean isLocal();

    void init(GL2 gl);

    void dispose(GL2 gl);

    void serialize(JSONObject jo);

}
