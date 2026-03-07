package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
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
    protected Vec3 computeDragPoint(Camera camera, Viewport vp, int x, int y) {
        return computePointSky(camera, vp, x, y);
    }

}
