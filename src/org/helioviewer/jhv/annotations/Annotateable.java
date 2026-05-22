package org.helioviewer.jhv.annotations;

import javax.annotation.Nullable;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.MapContext;
import org.helioviewer.jhv.display.ProjectionScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.BufVertex;

import org.json.JSONObject;

public interface Annotateable {

    @Nullable
    Object getData();

    void draw(MapContext ctx, Viewport vp, ProjectionScale scale, boolean active, BufVertex vexBuf);

    void drawTransformed(MapContext ctx, boolean active, BufVertex lineBuf, BufVertex centerBuf);

    void mousePressed(Camera camera, Viewport vp, int x, int y);

    void mouseDragged(Camera camera, Viewport vp, int x, int y);

    void mouseReleased();

    boolean beingDragged();

    boolean isDraggable();

    JSONObject toJson();

}
