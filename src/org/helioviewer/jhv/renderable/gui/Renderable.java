package org.helioviewer.jhv.renderable.gui;

import java.awt.Component;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public interface Renderable {

    void render(Camera camera, Viewport vp, GL2 gl);

    void renderScale(Camera camera, Viewport vp, GL2 gl);

    void renderFloat(Camera camera, Viewport vp, GL2 gl);

    void renderFullFloat(Camera camera, Viewport vp, GL2 gl);

    void renderMiniview(Camera camera, Viewport vp, GL2 gl);

    void prerender(GL2 gl);

    void remove(GL2 gl);

    Component getOptionsPanel();

    String getName();

    boolean isVisible(int i);

    boolean isVisible();

    int isVisibleIdx();

    void setVisible(boolean b);

    String getTimeString();

    boolean isDeletable();

    boolean isDownloading();

    void init(GL2 gl);

    void dispose(GL2 gl);

    void setVisible(int ctImages);

    void serialize(JSONObject jo);

    void serializeVisibility(JSONArray va);

    void deserializeVisibility(JSONArray va);

    boolean isLoadedForState();

}
