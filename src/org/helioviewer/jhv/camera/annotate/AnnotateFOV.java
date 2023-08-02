package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.json.JSONObject;

public class AnnotateFOV extends AbstractAnnotateable {

    private final FOVShape fov = new FOVShape();

    public AnnotateFOV(JSONObject jo) {
        super(jo);
    }

    public void zoom(Camera camera) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;
        double dx = 0.5 * (p1.x - p0.x);
        double dy = 0.5 * (p1.y - p0.y);

        camera.setTranslation(-(p0.x + dx), -(p0.y + dy));
        camera.resetDragRotation();
        camera.setFOV(2 * Math.atan2(Math.sqrt(dx * dx + dy * dy), camera.getViewpoint().distance));
    }

    @Override
    public void drawTransformed(boolean active, BufVertex lineBuf, BufVertex centerBuf) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        byte[] color = dragged ? dragColor : (active ? activeColor : baseColor);
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;
        double dx = 0.5 * (p1.x - p0.x);
        double dy = 0.5 * (p1.y - p0.y);

        fov.setCenter(p0.x + dx, p0.y + dy);
        fov.putCenter(centerBuf, color);
        fov.putRectLine(dx, dy, lineBuf, color);
    }

    @Override
    public void mousePressed(Camera camera, int x, int y) {
        Vec3 pt = computePointSky(camera, x, y);
        if (pt != null)
            dragStartPoint = pt;
    }

    @Override
    public void mouseDragged(Camera camera, int x, int y) {
        Vec3 pt = computePointSky(camera, x, y);
        if (pt != null)
            dragEndPoint = pt;
    }

    @Override
    public void mouseReleased() {
        if (beingDragged()) {
            startPoint = dragStartPoint;
            endPoint = dragEndPoint;
        }
        dragStartPoint = null;
        dragEndPoint = null;
    }

    @Override
    public boolean beingDragged() {
        return dragEndPoint != null && dragStartPoint != null;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

}
