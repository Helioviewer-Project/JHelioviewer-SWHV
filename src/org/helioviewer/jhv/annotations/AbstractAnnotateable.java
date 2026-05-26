package org.helioviewer.jhv.annotations;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.display.ViewportMath;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLRenderer;

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
        if (obj == null)
            return null;
        double lon = Math.toRadians(obj.optDouble("lon", 0)), lat = Math.toRadians(obj.optDouble("lat", 0));
        return SphericalCoords.unit(lon, lat);
    }

    static Vec3 interpolateSpherical(double t, double longitude1, double latitude1, double longitude2, double latitude2) {
        double longitude = (1 - t) * longitude1 + t * longitude2;
        double latitude = (1 - t) * latitude1 + t * latitude2;
        return SphericalCoords.unit(longitude, latitude);
    }

    @Nullable
    static Vec3 mouseToSurface(Camera camera, Viewport vp, int x, int y) {
        return Display.mode.mouseToSurface(camera, GLRenderer.getRenderView(), vp, Display.gridType, x, y);
    }

    @Nullable
    static Vec3 mouseToSky(Camera camera, Viewport vp, int x, int y) {
        return ViewportMath.unprojectToCurrentViewSphereOrPlane(camera, vp, GLRenderer.getRenderView().cameraWidth(vp.zoom), x, y);
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
    public void draw(MapView mv, Viewport vp, MapScale scale, boolean active, BufVertex vexBuf) {}

    @Override
    public void drawTransformed(MapView mv, boolean active, BufVertex lineBuf, BufVertex centerBuf) {}

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
