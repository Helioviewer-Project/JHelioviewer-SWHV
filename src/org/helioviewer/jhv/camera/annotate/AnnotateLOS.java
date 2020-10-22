package org.helioviewer.jhv.camera.annotate;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLHelper;
import org.json.JSONObject;

public class AnnotateLOS extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 2;

    private Quat posRotation; // plane-of-sky, should be saved

    public AnnotateLOS(JSONObject jo) {
        super(jo);
    }

    private static void drawLOS(Quat q, Viewport vp, Vec3 bp, BufVertex buf, byte[] color) {
        double delta = 2.5 * Math.PI / 180;
        Vec3 p1 = new Vec3(radius, bp.y + delta, bp.z);
        Vec3 p2 = new Vec3(radius, bp.y - delta, bp.z);
        Vec3 p3 = new Vec3(radius, bp.y, bp.z + delta);
        Vec3 p4 = new Vec3(radius, bp.y, bp.z - delta);

        interpolatedDraw(q, vp, p1, p2, buf, color);
        interpolatedDraw(q, vp, p3, p4, buf, color);
    }

    private static void interpolatedDraw(Quat q, Viewport vp, Vec3 p1s, Vec3 p2s, BufVertex buf, byte[] color) {
        Vec2 previous = null;
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolate(i / (double) SUBDIVISIONS, p1s, p2s);

            if (Display.mode == Display.DisplayMode.Orthographic) {
                if (i == 0) {
                    buf.putVertex(pc, Colors.Null);
                }
                buf.putVertex(pc, color);
                if (i == SUBDIVISIONS) {
                    buf.putVertex(pc, Colors.Null);
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

    @Nullable
    public Quat getPOSRotation() {
        return posRotation;
    }

    @Override
    public void draw(Quat q, Viewport vp, boolean active, BufVertex buf) {
        if (startPoint == null)
            return;

        byte[] color = active ? activeColor : baseColor;
        drawLOS(q, vp, toSpherical(startPoint), buf, color);
    }

    @Override
    public void mousePressed(Camera camera, int x, int y) {
        Vec3 pt = computePoint(camera, x, y);
        if (pt != null) {
            startPoint = pt;

            Vec3 posPoint = CameraHelper.getVectorFromSphere(camera, Display.getActiveViewport(), x, y, Quat.ZERO, true);
            if (posPoint != null) {
                double lon = Math.atan2(posPoint.x, posPoint.z);
                posRotation = new Quat(0, Math.PI / 2 - lon);
            }
        }
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
        return Interaction.AnnotationMode.LOS.toString();
    }

}
