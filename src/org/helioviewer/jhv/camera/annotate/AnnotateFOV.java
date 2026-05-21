package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapContext;
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

    public void zoom(Camera camera, Viewport vp) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;
        double dx = 0.5 * (p1.x - p0.x);
        double dy = 0.5 * (p1.y - p0.y);
        double halfHeight = Math.abs(dy);
        double halfWidth = Math.abs(dx) / vp.aspect;
        double halfSize = 1.1 * Math.max(halfHeight, halfWidth); // give some margin

        camera.setTranslation(-(p0.x + dx), -(p0.y + dy));
        Position viewpoint = Display.getViewpoint();
        camera.resetDragRotation(viewpoint);
        camera.setFOV(2 * Math.atan2(halfSize, viewpoint.distance), viewpoint);
    }

    @Override
    public void drawTransformed(MapContext ctx, boolean active, BufVertex lineBuf, BufVertex centerBuf) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        byte[] color = dragged ? dragColor : (active ? activeColor : baseColor);
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;
        double dx = 0.5 * (p1.x - p0.x);
        double dy = 0.5 * (p1.y - p0.y);

        fov.setCenter(p0.x + dx, p0.y + dy);
        fov.putCenter(ctx.isHpc(), color, centerBuf);
        fov.putRectLine(dx, dy, ctx.isHpc(), color, lineBuf);
    }

    @Override
    protected Vec3 computeDragPoint(Camera camera, Viewport vp, int x, int y) {
        return mouseToSky(camera, vp, x, y);
    }

}
