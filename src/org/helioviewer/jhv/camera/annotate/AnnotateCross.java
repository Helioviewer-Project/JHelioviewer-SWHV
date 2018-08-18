package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.FloatArray;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.InteractionAnnotate.AnnotationMode;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class AnnotateCross extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 2;

    private final GLSLLine line = new GLSLLine();

    public AnnotateCross(JSONObject jo) {
        super(jo);
    }

    @Override
    public void init(GL2 gl) {
        line.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        line.dispose(gl);
    }

    private static void drawCross(Camera camera, Viewport vp, Vec3 bp, FloatArray pos, FloatArray col, float[] color) {
        double delta = 2.5 * Math.PI / 180;
        Vec3 p1 = new Vec3(radius, bp.y + delta, bp.z);
        Vec3 p2 = new Vec3(radius, bp.y - delta, bp.z);
        Vec3 p3 = new Vec3(radius, bp.y, bp.z + delta);
        Vec3 p4 = new Vec3(radius, bp.y, bp.z - delta);

        interpolatedDraw(camera, vp, p1, p2, pos, col, color);
        interpolatedDraw(camera, vp, p3, p4, pos, col, color);
    }

    private static void interpolatedDraw(Camera camera, Viewport vp, Vec3 p1s, Vec3 p2s, FloatArray pos, FloatArray col, float[] color) {
        Vec2 previous = null;
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            Vec3 pc = interpolate(i / (double) SUBDIVISIONS, p1s, p2s);

            if (Display.mode == Display.DisplayMode.Orthographic) {
                if (i == 0) {
                    pos.put4f(pc);
                    col.put4f(BufferUtils.colorNull);
                }
                pos.put4f(pc);
                col.put4f(color);
                if (i == SUBDIVISIONS) {
                    pos.put4f(pc);
                    col.put4f(BufferUtils.colorNull);
                }
            } else {
                pc.y = -pc.y;
                if (i == 0) {
                    GLHelper.drawVertex(camera, vp, pc, previous, pos, col, BufferUtils.colorNull);
                }
                previous = GLHelper.drawVertex(camera, vp, pc, previous, pos, col, color);
                if (i == SUBDIVISIONS) {
                    GLHelper.drawVertex(camera, vp, pc, previous, pos, col, BufferUtils.colorNull);
                }
            }
        }
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl, boolean active) {
        if (startPoint == null)
            return;

        float[] color = active ? activeColor : baseColor;

        FloatArray pos = new FloatArray();
        FloatArray col = new FloatArray();

        drawCross(camera, vp, toSpherical(startPoint), pos, col, color);
        line.setData(gl, pos.toBuffer(), col.toBuffer());
//      gl.glDisable(GL2.GL_DEPTH_TEST);
        line.render(gl, vp, LINEWIDTH);
//      gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    @Override
    public void mousePressed(Camera camera, int x, int y) {
        Vec3 pt = computePoint(camera, x, y);
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
        return AnnotationMode.Cross.toString();
    }

}
