package org.helioviewer.jhv.annotation;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;

import org.json.JSONObject;

final class AnnotateFOV extends AbstractAnnotateable {

    AnnotateFOV(JSONObject jo) {
        super(jo);
    }

    void zoom(Viewport vp) {
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

        DisplayController.zoomToFovAnnotation(p0.x + dx, p0.y + dy, halfSize);
    }

    @Override
    public void drawTransformed(MapView mv, boolean active, BufVertex lineBuf, BufVertex centerBuf) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        double lineThickness = thickness(active);
        byte[] color = color(dragged);
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;
        double dx = 0.5 * (p1.x - p0.x);
        double dy = 0.5 * (p1.y - p0.y);
        double centerX = p0.x + dx;
        double centerY = p0.y + dy;

        FOVShape.putCenter(centerX, centerY, mv.isHpc(), lineThickness, color, centerBuf);
        FOVShape.putRectLine(centerX, centerY, dx, dy, mv.isHpc(), lineThickness, color, lineBuf);
    }

    @Override
    protected Vec3 computeDragPoint(Viewport vp, int x, int y) {
        return mouseToSky(vp, x, y);
    }

}
