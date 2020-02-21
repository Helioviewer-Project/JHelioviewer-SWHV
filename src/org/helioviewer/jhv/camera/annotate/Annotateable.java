package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.opengl.BufVertex;
import org.json.JSONObject;

public interface Annotateable {

    void draw(Quat q, Viewport vp, boolean active, BufVertex buf);

    void drawTransformed(boolean active, BufVertex lineBuf, BufVertex centerBuf);

    void mousePressed(Camera camera, int x, int y);

    void mouseDragged(Camera camera, int x, int y);

    void mouseReleased();

    boolean beingDragged();

    boolean isDraggable();

    String getType();

    JSONObject toJson();

}
