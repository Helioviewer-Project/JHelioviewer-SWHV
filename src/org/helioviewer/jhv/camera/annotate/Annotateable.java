package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.base.Buf;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.json.JSONObject;

public interface Annotateable {

    void render(Camera camera, Viewport vp, boolean active, Buf buf);

    void renderTransformed(Camera camera, boolean active, Buf lineBuf, Buf centerBuf);

    void mousePressed(Camera camera, int x, int y);

    void mouseDragged(Camera camera, int x, int y);

    void mouseReleased();

    boolean beingDragged();

    boolean isDraggable();

    String getType();

    JSONObject toJson();

}
