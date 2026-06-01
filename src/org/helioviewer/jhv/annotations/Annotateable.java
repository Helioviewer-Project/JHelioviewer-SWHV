package org.helioviewer.jhv.annotations;

import javax.annotation.Nullable;

import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.BufVertex;

import org.json.JSONObject;

public interface Annotateable {

    @Nullable
    Object getData();

    void draw(MapView mv, Viewport vp, MapScale scale, boolean active, BufVertex vexBuf);

    void drawTransformed(MapView mv, boolean active, BufVertex lineBuf, BufVertex centerBuf);

    void mousePressed(Viewport vp, int x, int y);

    void mouseDragged(Viewport vp, int x, int y);

    void mouseReleased();

    boolean beingDragged();

    boolean isDraggable();

    double thickness();

    byte[] baseColor();

    JSONObject toJson();

}
