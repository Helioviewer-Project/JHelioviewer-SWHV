package org.helioviewer.jhv.camera.annotate;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLHelper;
import org.json.JSONObject;

public class AnnotateCross extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 2;

    public AnnotateCross(JSONObject jo) {
        super(jo);
    }

    public static void drawCross(Quat q, Viewport vp, Vec3 bp, BufVertex buf, byte[] color) {
        double delta = 2.5 * Math.PI / 180;
        Vec3 p1 = new Vec3(1, bp.y + delta, bp.z);
        Vec3 p2 = new Vec3(1, bp.y - delta, bp.z);
        Vec3 p3 = new Vec3(1, bp.y, bp.z + delta);
        Vec3 p4 = new Vec3(1, bp.y, bp.z - delta);

        interpolatedDraw(q, vp, p1, p2, buf, color);
        interpolatedDraw(q, vp, p3, p4, buf, color);
    }

    private static void interpolatedDraw(Quat q, Viewport vp, Vec3 p1s, Vec3 p2s, BufVertex buf, byte[] color) {
        Vec2 previous = null;
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolate(i / (double) SUBDIVISIONS, p1s, p2s);

            if (Display.mode == Display.ProjectionMode.Orthographic) {
                if (i == 0) {
                    putSphere(pc, buf, Colors.Null);
                }
                putSphere(pc, buf, color);
                if (i == SUBDIVISIONS) {
                    putSphere(pc, buf, Colors.Null);
                }
            } else {
                pc.y = -pc.y;
                if (i == 0) {
                    GLHelper.drawVertex(q, vp, pc, previous, buf, Colors.Null);
                }
                previous = GLHelper.drawVertex(q, vp, pc, previous, buf, color);
                if (i == SUBDIVISIONS) {
                    GLHelper.drawVertex(q, vp, pc, previous, buf, Colors.Null);
                }
            }
        }
    }

    @Override
    public void draw(Quat q, Viewport vp, boolean active, BufVertex buf) {
        if (startPoint == null)
            return;

        byte[] color = active ? activeColor : baseColor;
        drawCross(q, vp, toSpherical(startPoint), buf, color);
    }

    @Override
    public void mousePressed(Camera camera, int x, int y) {
        Vec3 pt = computePointSun(camera, x, y);
        if (pt != null)
            startPoint = pt;
    }

    @Override
    public void mouseDragged(Camera camera, int x, int y) {
    }

    @Override
    public void mouseReleased() {
    }

    @Override
    public boolean beingDragged() {
        return true;
    }

    @Override
    public boolean isDraggable() {
        return false;
    }

    @Override
    public String getType() {
        return Interaction.AnnotationMode.Cross.toString();
    }

    @Nullable
    @Override
    public Object getData() {
        return startPoint;
    }

}
