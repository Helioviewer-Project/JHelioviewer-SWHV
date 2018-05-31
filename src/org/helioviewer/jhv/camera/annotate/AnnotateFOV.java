package org.helioviewer.jhv.camera.annotate;

import javax.annotation.Nullable;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.InteractionAnnotate.AnnotationMode;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLHelper;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class AnnotateFOV extends AbstractAnnotateable {

    public AnnotateFOV(JSONObject jo) {
        super(jo);
    }

    @Nullable
    private Vec3 computePointFOV(Camera camera, int x, int y) {
        return CameraHelper.getVectorFromSphereOrPlane(camera, Display.getActiveViewport(), x, y, camera.getCurrentDragRotation());
    }

    private void drawRectangle(Camera camera, Viewport vp, GL2 gl, Vec3 bp, Vec3 ep) {
        if (bp.z * ep.z < 0) {
            if (ep.z < bp.z && bp.z > Math.PI / 2)
                ep.z += 2 * Math.PI;
            else if (ep.z > bp.z && bp.z < -Math.PI / 2)
                bp.z += 2 * Math.PI;
        }

        Vec3 p2 = new Vec3(radius, ep.y, bp.z);
        Vec3 p4 = new Vec3(radius, bp.y, ep.z);

        gl.glBegin(GL2.GL_LINE_STRIP);
        interpolatedDraw(camera, vp, gl, bp, p2);
        interpolatedDraw(camera, vp, gl, p2, ep);
        interpolatedDraw(camera, vp, gl, ep, p4);
        interpolatedDraw(camera, vp, gl, p4, bp);
        gl.glEnd();
    }

    private void interpolatedDraw(Camera camera, Viewport vp, GL2 gl, Vec3 p1s, Vec3 p2s) {
        double delta = 2.5 * Math.PI / 180;
        int subdivisions = (int) Math.max(Math.abs(p1s.y - p2s.y) / delta, Math.abs(p1s.z - p2s.z) / delta);
        subdivisions = Math.max(1, subdivisions);

        Vec2 previous = null;
        for (double i = 0; i <= subdivisions; i++) {
            double t = i / subdivisions;
            double y = (1 - t) * p1s.y + t * p2s.y;
            double z = (1 - t) * p1s.z + t * p2s.z;

            Vec3 pc = toCart(y, z);
            if (Display.mode == Display.DisplayMode.Orthographic) {
                gl.glVertex3f((float) pc.x, (float) pc.y, (float) pc.z);
            } else {
                pc.y = -pc.y;
                previous = GLHelper.drawVertex(camera, vp, gl, pc, previous);
            }
        }
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl, boolean active) {
        if ((startPoint == null || endPoint == null) && !beingDragged())
            return;

        gl.glLineWidth(lineWidth);

        gl.glPushMatrix();
        gl.glMultMatrixd(camera.getViewpoint().toQuat().toMatrix().transpose().m, 0);

        if (beingDragged()) {
            gl.glColor3f(dragColor[0], dragColor[1], dragColor[2]);
            drawRectangle(camera, vp, gl, toSpherical(dragStartPoint), toSpherical(dragEndPoint));
        } else {
            if (active)
                gl.glColor3f(activeColor[0], activeColor[1], activeColor[2]);
            else
                gl.glColor3f(baseColor[0], baseColor[1], baseColor[2]);
            drawRectangle(camera, vp, gl, toSpherical(startPoint), toSpherical(endPoint));
        }

        gl.glPopMatrix();
    }

    @Override
    public void mousePressed(Camera camera, int x, int y) {
        Vec3 pt = computePointFOV(camera, x, y);
        if (pt != null)
            dragStartPoint = pt;
    }

    @Override
    public void mouseDragged(Camera camera, int x, int y) {
        Vec3 pt = computePointFOV(camera, x, y);
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
        return AnnotationMode.FOV.toString();
    }

}
