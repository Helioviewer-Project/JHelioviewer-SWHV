package org.helioviewer.jhv.camera.annotate;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Buf;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.InteractionAnnotate.AnnotationMode;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.position.Position;
import org.json.JSONObject;

public class AnnotateFOV extends AbstractAnnotateable {

    private final FOVShape fov = new FOVShape();

    public AnnotateFOV(JSONObject jo) {
        super(jo);
    }

    @Nullable
    private static Vec3 computePointFOV(Camera camera, int x, int y) {
        return CameraHelper.getVectorFromSphereOrPlane(camera, Display.getActiveViewport(), x, y, camera.getCurrentDragRotation());
    }

    public void zoom(Camera camera) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;
        double dx = (p1.x - p0.x) / 2;
        double dy = (p1.y - p0.y) / 2;

        camera.setCurrentTranslation(-(p0.x + dx), -(p0.y + dy));
        camera.setFOV(2 * Math.atan2(Math.sqrt(dx * dx + dy * dy), camera.getViewpoint().distance));
    }

    @Override
    public void renderTransformed(Position viewpoint, boolean active, Buf lineBuf, Buf centerBuf) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        byte[] color = dragged ? dragColor : (active ? activeColor : baseColor);
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;
        double dx = (p1.x - p0.x) / 2;
        double dy = (p1.y - p0.y) / 2;

        fov.setCenter(p0.x + dx, p0.y + dy);
        fov.setTAngles(dx / viewpoint.distance, dy / viewpoint.distance);

        fov.putCenter(centerBuf, color);
        fov.putLine(viewpoint.distance, lineBuf, color);
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
