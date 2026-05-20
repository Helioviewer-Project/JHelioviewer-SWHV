package org.helioviewer.jhv.math;

import org.json.JSONArray;

public class Vec2 {

    public static final Vec2 ZERO = new Vec2(0, 0);
    public static final Vec2 NAN = new Vec2(Double.NaN, Double.NaN);

    public final double x;
    public final double y;

    public Vec2(double _x, double _y) {
        x = _x;
        y = _y;
    }

    public JSONArray toJson() {
        return new JSONArray(new double[]{x, y});
    }

    public static Vec2 fromJson(JSONArray ja) {
        try {
            double x = ja.getDouble(0);
            double y = ja.getDouble(1);
            return Double.isFinite(x) && Double.isFinite(y) ? new Vec2(x, y) : ZERO;
        } catch (Exception e) {
            return ZERO;
        }
    }

}
