package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.Interaction;
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

    @Override
    public String getType() {
        return Interaction.AnnotationMode.Line.toString();
    }

}
