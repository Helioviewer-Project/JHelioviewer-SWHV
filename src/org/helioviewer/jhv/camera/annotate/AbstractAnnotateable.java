package org.helioviewer.jhv.camera.annotate;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.json.JSONArray;
import org.json.JSONObject;

abstract class AbstractAnnotateable implements Annotateable {

    static final byte[] activeColor = Colors.Red;
    static final byte[] dragColor = Colors.Green;
    static final byte[] baseColor = Colors.Blue;

    private static final double radius = Sun.Radius * 1.01;

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

    static Vec3 toSpherical(Vec3 p) {
        double len = p.length();
        return new Vec3(len, Math.acos(p.y / len), Math.atan2(p.x, p.z));
    }

    private static JSONObject toPointJson(Vec3 p) {
        double len = p.length();
        double lon = Math.atan2(p.x, p.z);
        double lat = Math.asin(p.y / len);
        return new JSONObject().put("lon", Math.toDegrees(lon < 0 ? lon + 2 * Math.PI : lon)).put("lat", Math.toDegrees(lat));
    }

    private static Vec3 toCart(double y, double z) {
        return new Vec3(Math.sin(y) * Math.sin(z), Math.cos(y), Math.sin(y) * Math.cos(z));
    }

    private static Vec3 fromPointJson(JSONObject jo, String name) {
        JSONArray arr = jo.optJSONArray(name);
        if (arr != null)
            return Vec3.fromJson(arr);
        JSONObject obj = jo.optJSONObject(name);
        if (obj == null || !obj.has("lon") || !obj.has("lat"))
            return null;
        double lon = Math.toRadians(obj.getDouble("lon")), lat = Math.toRadians(obj.getDouble("lat"));
        return new Vec3(Math.cos(lat) * Math.sin(lon), Math.sin(lat), Math.cos(lat) * Math.cos(lon));
    }

    static Vec3 interpolate(double t, Vec3 point1, Vec3 point2) {
        double y = (1 - t) * point1.y + t * point2.y;
        double z = (1 - t) * point1.z + t * point2.z;
        return toCart(y, z);
    }

    static void putSphere(Vec3 v, BufVertex buf, byte[] color) {
        buf.putVertex((float) (v.x * radius), (float) (v.y * radius), (float) (v.z * radius), 1, color);
    }

    @Nullable
    static Vec3 computePointSun(Camera camera, int x, int y) {
        if (Display.mode == Display.ProjectionMode.Orthographic) {
            Quat q = camera.getViewpoint().toQuat();
            return CameraHelper.getVectorFromSphere(camera, Display.getActiveViewport(), x, y, q, true);
        } else {
            Quat q = Display.gridType.toCarrington(camera.getViewpoint()); //!
            return Display.mode.transformInverse(q, Display.mode.scale.mouseToGrid(x, y, Display.getActiveViewport(), camera, Display.gridType));
        }
    }

    @Nullable
    static Vec3 computePointSky(Camera camera, int x, int y) {
        return CameraHelper.getVectorFromSphereOrPlane(camera, Display.getActiveViewport(), x, y, camera.getDragRotation());
    }

    @Nullable
    @Override
    public Object getData() {
        return null;
    }

    @Override
    public void draw(Quat q, Viewport vp, boolean active, BufVertex buf) {
    }

    @Override
    public void drawTransformed(boolean active, BufVertex lineBuf, BufVertex centerBuf) {
    }

    @Override
    public JSONObject toJson() {
        JSONObject jo = new JSONObject().put("type", Interaction.AnnotationMode.modes.get(this.getClass()));
        if (startPoint != null)
            jo.put("startPoint", toPointJson(startPoint));
        if (endPoint != null)
            jo.put("endPoint", toPointJson(endPoint));
        return jo;
    }

}
