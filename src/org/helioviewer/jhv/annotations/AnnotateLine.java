package org.helioviewer.jhv.annotations;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;

import org.json.JSONObject;

final class AnnotateLine extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 32;

    AnnotateLine(JSONObject jo) {
        super(jo);
    }

    private static void drawLine(MapView ctx, double centerX, double centerY, double bw, double bh, byte[] color, BufVertex vexBuf) {
        boolean flat = ctx.isHpc();
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            double x = -bw + 2 * bw / SUBDIVISIONS * i + centerX;
            double y = -bh + 2 * bh / SUBDIVISIONS * i + centerY;
            double z = FOVShape.computeZ(x, y, flat);
            if (i == 0) { // first
                vexBuf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
            vexBuf.putVertex((float) x, (float) y, (float) z, 1, color);
            if (i == SUBDIVISIONS) { // last
                vexBuf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
        }
    }

    @Override
    public void drawTransformed(MapView ctx, boolean active, BufVertex lineBuf, BufVertex centerBuf) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        byte[] color = dragged ? dragColor : (active ? activeColor : baseColor);
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;
        double dx = 0.5 * (p1.x - p0.x);
        double dy = 0.5 * (p1.y - p0.y);

        drawLine(ctx, p0.x + dx, p0.y + dy, dx, dy, color, lineBuf);
    }

    @Override
    protected Vec3 computeDragPoint(Camera camera, Viewport vp, int x, int y) {
        return mouseToSky(camera, vp, x, y);
    }

}
