package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.MapContext;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

import org.json.JSONObject;

public class AnnotateCross extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 2;

    public AnnotateCross(JSONObject jo) {
        super(jo);
    }

    public static void drawCross(MapContext ctx, double longitude, double latitude, byte[] color, BufVertex vexBuf) {
        double delta = 2.5 * Math.PI / 180;
        interpolatedDraw(ctx, longitude + delta, latitude, longitude - delta, latitude, color, vexBuf);
        interpolatedDraw(ctx, longitude, latitude + delta, longitude, latitude - delta, color, vexBuf);
    }

    private static void interpolatedDraw(MapContext ctx, double longitude1, double latitude1, double longitude2, double latitude2, byte[] color, BufVertex vexBuf) {
        Vec2 previous = null;
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolateSpherical(i / (double) SUBDIVISIONS, longitude1, latitude1, longitude2, latitude2);
            previous = ctx.emitMapVertex(pc, previous, i == 0, i == SUBDIVISIONS, ANNOTATION_RADIUS, color, vexBuf);
        }
    }

    @Override
    public void draw(MapContext ctx, boolean active, BufVertex vexBuf) {
        if (startPoint == null)
            return;

        byte[] color = active ? activeColor : baseColor;
        drawCross(ctx, SphericalCoords.longitude(startPoint), SphericalCoords.latitude(startPoint), color, vexBuf);
    }

    @Override
    public void mousePressed(Camera camera, Viewport vp, int x, int y) {
        Vec3 pt = mouseToSurface(camera, vp, x, y);
        if (pt != null)
            startPoint = pt;
    }

    @Override
    public void mouseDragged(Camera camera, Viewport vp, int x, int y) {}

    @Override
    public void mouseReleased() {}

    @Override
    public boolean beingDragged() {
        return startPoint != null;
    }

    @Override
    public boolean isDraggable() {
        return false;
    }

}
