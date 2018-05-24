package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.display.Viewport;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public interface Annotateable {

    void render(Viewport vp, GL2 gl, boolean active);

    void mousePressed(int x, int y);

    void mouseDragged(int x, int y);

    void mouseReleased();

    boolean beingDragged();

    boolean isDraggable();

    String getType();

    JSONObject toJson();

}
