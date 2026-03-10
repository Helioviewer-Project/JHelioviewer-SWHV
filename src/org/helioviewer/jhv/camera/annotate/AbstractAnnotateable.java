package org.helioviewer.jhv.camera.annotate;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.json.JSONArray;
import org.json.JSONObject;

abstract class AbstractAnnotateable implements Annotateable {

    static final byte[] activeColor = Colors.Red;
    static final byte[] dragColor = Colors.Green;
    static final byte[] baseColor = Colors.Blue;

    protected static final double ANNOTATION_RADIUS = Sun.Radius * 1.01;

    Vec3 startPoint;
    Vec3 endPoint;

    Vec3 dragStartPoint;
    Vec3 dragEndPoint;

    AbstractAnnotateable(JSONObject jo) {
        if (jo != null) {
            startPoint = fromPointJson(jo, "startPoint");
            endPoint = fromPointJson(jo, "endPoint");
        }
    }

    private static JSONObject toPointJson(Vec3 p) {
        double lon = SphericalCoords.longitude(p);
        double lat = SphericalCoords.latitude(p);
        return new JSONObject().put("lon", Math.toDegrees(lon < 0 ? lon + 2 * Math.PI : lon)).put("lat", Math.toDegrees(lat));
    }

    private static Vec3 fromPointJson(JSONObject jo, String name) {
        JSONArray arr = jo.optJSONArray(name);
        if (arr != null)
            return Vec3.fromJson(arr);
        JSONObject obj = jo.optJSONObject(name);
        if (obj == null || !obj.has("lon") || !obj.has("lat"))
            return null;
        double lon = Math.toRadians(obj.getDouble("lon")), lat = Math.toRadians(obj.getDouble("lat"));
        return SphericalCoords.unit(lon, lat);
    }

    static Vec3 interpolate(double t, Vec3 point1, Vec3 point2) {
        double longitude = (1 - t) * point1.y + t * point2.y;
        double latitude = (1 - t) * point1.z + t * point2.z;
        return SphericalCoords.unit(longitude, latitude);
    }

    static Vec3 annotationSpherical(Vec3 point) {
        return new Vec3(
                SphericalCoords.radius(point),
                SphericalCoords.longitude(point),
                SphericalCoords.latitude(point));
    }

    @Nullable
    static Vec3 computePointSun(Camera camera, Viewport vp, int x, int y) {
        return Display.mode.unprojectMouse(camera, vp, x, y, Display.gridType);
    }

    @Nullable
    static Vec3 computePointSky(Camera camera, Viewport vp, int x, int y) {
        return CameraHelper.unprojectToCurrentViewSphereOrPlane(camera, vp, x, y);
    }

    @Nullable
    @Override
    public Object getData() {
        return null;
    }

    @Nullable
    protected Vec3 computeDragPoint(Camera camera, Viewport vp, int x, int y) {
        return null;
    }

    @Override
    public void draw(Position viewpoint, GridType gridType, Viewport vp, boolean active, BufVertex buf) {
    }

    @Override
    public void drawTransformed(boolean active, BufVertex lineBuf, BufVertex centerBuf) {
    }

    @Override
    public void mousePressed(Camera camera, Viewport vp, int x, int y) {
        if (!isDraggable())
            return;

        Vec3 pt = computeDragPoint(camera, vp, x, y);
        if (pt != null)
            dragStartPoint = pt;
    }

    @Override
    public void mouseDragged(Camera camera, Viewport vp, int x, int y) {
        if (!isDraggable())
            return;

        Vec3 pt = computeDragPoint(camera, vp, x, y);
        if (pt != null)
            dragEndPoint = pt;
    }

    @Override
    public void mouseReleased() {
        if (!isDraggable())
            return;

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
    public JSONObject toJson() {
        JSONObject jo = new JSONObject().put("type", getTypeName());
        if (startPoint != null)
            jo.put("startPoint", toPointJson(startPoint));
        if (endPoint != null)
            jo.put("endPoint", toPointJson(endPoint));
        return jo;
    }

    private String getTypeName() {
        String className = getClass().getSimpleName();
        return className.startsWith("Annotate") ? className.substring("Annotate".length()) : className;
    }

}
