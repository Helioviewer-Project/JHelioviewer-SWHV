package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.base.Buf;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public interface Annotateable {

    void init(GL2 gl);

    void dispose(GL2 gl);

    void render(Camera camera, Viewport vp, boolean active, Buf buf);

    void renderTransformed(Camera camera, Viewport vp, GL2 gl, boolean active, Buf buf);

    void mousePressed(Camera camera, int x, int y);

    void mouseDragged(Camera camera, int x, int y);

    void mouseReleased();

    boolean beingDragged();

    boolean isDraggable();

    String getType();

    JSONObject toJson();

}
