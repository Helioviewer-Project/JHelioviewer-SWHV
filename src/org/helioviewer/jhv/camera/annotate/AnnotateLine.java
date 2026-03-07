package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.json.JSONObject;

public class AnnotateLine extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 32;

    public AnnotateLine(JSONObject jo) {
        super(jo);
    }

    private static void drawLine(double centerX, double centerY, double bw, double bh, BufVertex buf, byte[] color) {
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            double x = -bw + 2 * bw / SUBDIVISIONS * i + centerX;
            double y = -bh + 2 * bh / SUBDIVISIONS * i + centerY;
            double z = FOVShape.computeZ(x, y);
            if (i == 0) { // first
                buf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
            buf.putVertex((float) x, (float) y, (float) z, 1, color);
            if (i == SUBDIVISIONS) { // last
                buf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
        }
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

        drawLine(p0.x + dx, p0.y + dy, dx, dy, lineBuf, color);
    }

    @Override
    protected Vec3 computeDragPoint(Camera camera, Viewport vp, int x, int y) {
        return computePointSky(camera, vp, x, y);
    }

}
