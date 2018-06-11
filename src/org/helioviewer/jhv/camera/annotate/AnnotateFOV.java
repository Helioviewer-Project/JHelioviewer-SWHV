package org.helioviewer.jhv.camera.annotate;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.InteractionAnnotate.AnnotationMode;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.GLInfo;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class AnnotateFOV extends AbstractAnnotateable {

    private final FOVShape fov = new FOVShape(thickness);

    public AnnotateFOV(JSONObject jo) {
        super(jo);
    }

    @Override
    public void init(GL2 gl) {
        fov.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        fov.dispose(gl);
    }

    @Nullable
    private static Vec3 computePointFOV(Camera camera, int x, int y) {
        return CameraHelper.getVectorFromSphereOrPlane(camera, Display.getActiveViewport(), x, y, camera.getCurrentDragRotation());
    }

    public void zoom(Camera camera) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        Position viewpoint = camera.getViewpoint();
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;
        double dx = (p1.x - p0.x) / 2;
        double dy = (p1.y - p0.y) / 2;

        camera.setCurrentTranslation(-(p0.x + dx), -(p0.y + dy));
        camera.setFOV(2 * Math.atan2(Math.sqrt(dx * dx + dy * dy), viewpoint.distance));
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl, boolean active) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        double pointFactor = GLInfo.pixelScale[0] / (2 * camera.getFOV());
        Position viewpoint = camera.getViewpoint();
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;
        double dx = (p1.x - p0.x) / 2;
        double dy = (p1.y - p0.y) / 2;

        Transform.pushView();
        Transform.mulViewInverse(viewpoint.toQuat());
        {
            fov.setCenter(p0.x + dx, p0.y + dy);
            fov.setTAngles(dx / viewpoint.distance, dy / viewpoint.distance);
            fov.render(gl, viewpoint.distance, vp.aspect, pointFactor, active);
        }
        Transform.popView();
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
