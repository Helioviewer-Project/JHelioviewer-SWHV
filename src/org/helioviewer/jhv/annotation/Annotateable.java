package org.helioviewer.jhv.annotation;

import javax.annotation.Nullable;

import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.BufVertex;

import org.json.JSONObject;

public interface Annotateable {

    @Nullable
    String getData();

    void draw(MapView mv, Viewport vp, boolean pending, BufVertex vexBuf);

    void drawTransformed(MapView mv, double lineThickness, boolean pending, BufVertex lineBuf, BufVertex centerBuf);

    void mousePressed(Viewport vp, int x, int y);

    void mouseDragged(Viewport vp, int x, int y);

    boolean isComplete();

    boolean isDraggable();

    double thickness(boolean active);

    JSONObject toJson();

}
