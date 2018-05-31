package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.InteractionAnnotate.AnnotationMode;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Vec3;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class AnnotateCross extends AbstractAnnotateable {

    public AnnotateCross(JSONObject jo) {
        super(jo);
    }

    private void drawCross(Camera camera, Viewport vp, GL2 gl, Vec3 bp) {
        double delta = 2.5 * Math.PI / 180;
        Vec3 p1 = new Vec3(radius, bp.y + delta, bp.z);
        Vec3 p2 = new Vec3(radius, bp.y - delta, bp.z);
        Vec3 p3 = new Vec3(radius, bp.y, bp.z + delta);
        Vec3 p4 = new Vec3(radius, bp.y, bp.z - delta);
        gl.glDisable(GL2.GL_DEPTH_TEST);
        interpolatedLineDraw(camera, vp, gl, p1, p2, 2);
        interpolatedLineDraw(camera, vp, gl, p3, p4, 2);
        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl, boolean active) {
        if (startPoint == null)
            return;

        gl.glLineWidth(lineWidth);

        if (active)
            gl.glColor3f(activeColor[0], activeColor[1], activeColor[2]);
        else
            gl.glColor3f(baseColor[0], baseColor[1], baseColor[2]);
        drawCross(camera, vp, gl, toSpherical(startPoint));
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
