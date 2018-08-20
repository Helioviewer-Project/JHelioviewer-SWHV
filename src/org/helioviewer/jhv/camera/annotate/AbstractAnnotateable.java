package org.helioviewer.jhv.camera.annotate;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.base.scale.GridType;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.json.JSONArray;
import org.json.JSONObject;

abstract class AbstractAnnotateable implements Annotateable {

    static final byte[] activeColor = Colors.Red;
    static final byte[] dragColor = Colors.Yellow;
    static final byte[] baseColor = Colors.Blue;

    static final double LINEWIDTH = 0.002;
    static final double radius = Sun.Radius * 1.01;

    Vec3 startPoint;
    Vec3 endPoint;

    Vec3 dragStartPoint;
    Vec3 dragEndPoint;

    AbstractAnnotateable(JSONObject jo) {
        if (jo != null) {
            JSONArray jaStart = jo.optJSONArray("startPoint");
            if (jaStart != null)
                startPoint = Vec3.fromJson(jaStart);

            JSONArray jaEnd = jo.optJSONArray("endPoint");
            if (jaEnd != null)
                endPoint = Vec3.fromJson(jaEnd);
        }
    }

    static Vec3 toSpherical(Vec3 p) {
        double len = p.length();
        return new Vec3(len, Math.acos(p.y / len), Math.atan2(p.x, p.z));
    }

    private static Vec3 toCart(double y, double z) {
        return new Vec3(radius * Math.sin(y) * Math.sin(z),
                        radius * Math.cos(y),
                        radius * Math.sin(y) * Math.cos(z));
    }

    static Vec3 interpolate(double t, Vec3 point1, Vec3 point2) {
        double y = (1 - t) * point1.y + t * point2.y;
        double z = (1 - t) * point1.z + t * point2.z;
        return toCart(y, z);
    }

    @Nullable
    static Vec3 computePoint(Camera camera, int x, int y) {
        Quat frame = camera.getViewpoint().toQuat();
        if (Display.mode == Display.DisplayMode.Orthographic) {
            return CameraHelper.getVectorFromSphere(camera, Display.getActiveViewport(), x, y, frame, true);
        } else {
            return Display.mode.xform.transformInverse(frame, Display.mode.scale.mouseToGrid(x, y, Display.getActiveViewport(), camera, GridType.Viewpoint));
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject jo = new JSONObject().put("type", getType());
        if (startPoint != null)
            jo.put("startPoint", startPoint.toJson());
        if (endPoint != null)
            jo.put("endPoint", endPoint.toJson());
        return jo;
    }

}
