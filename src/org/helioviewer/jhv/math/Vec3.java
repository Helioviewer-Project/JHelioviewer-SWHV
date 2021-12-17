package org.helioviewer.jhv.math;

import org.helioviewer.jhv.Log;
import org.json.JSONArray;

public class Vec3 {

    public static final Vec3 ZERO = new Vec3(0, 0, 0);
    public static final Vec3 XAxis = new Vec3(1, 0, 0);
    public static final Vec3 YAxis = new Vec3(0, 1, 0);
    public static final Vec3 ZAxis = new Vec3(0, 0, 1);

    public double x;
    public double y;
    public double z;

    public Vec3(double _x, double _y, double _z) {
        x = _x;
        y = _y;
        z = _z;
    }

    public Vec3() {
        x = 0;
        y = 0;
        z = 0;
    }

    public void add(Vec3 vec) {
        x += vec.x;
        y += vec.y;
        z += vec.z;
    }

    public static Vec3 multiply(Vec3 vec1, double s) {
        return new Vec3(vec1.x * s, vec1.y * s, vec1.z * s);
    }

    public static double dot(Vec3 u, Vec3 v) {
        return (u.x * v.x) + (u.y * v.y) + (u.z * v.z);
    }

    public static Vec3 cross(Vec3 u, Vec3 v) {
        return new Vec3(u.y * v.z - u.z * v.y, u.z * v.x - u.x * v.z, u.x * v.y - u.y * v.x);
    }

    public double length() {
        double absmax = Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z));
        if (absmax == 0.0)
            return 0.0;

        double tmpx = x / absmax;
        double tmpy = y / absmax;
        double tmpz = z / absmax;
        return absmax * Math.sqrt(tmpx * tmpx + tmpy * tmpy + tmpz * tmpz);
    }

    public double length2() {
        return x * x + y * y + z * z;
    }

    public void normalize() {
        double len = Math.sqrt(x * x + y * y + z * z);
        if (len == 0.0)
            return;

        x /= len;
        y /= len;
        z /= len;

        len = Math.sqrt(x * x + y * y + z * z);
        if (len <= 1.0)
            return;

        // errors up to 2ulp found in testing
        len = Math.nextAfter(len, len + 1.0);
        x /= len;
        y /= len;
        z /= len;

        len = Math.sqrt(x * x + y * y + z * z);
        if (len > 1 || Double.isNaN(len)) {
            Log.error("Normalized to bigger than 1: please report. Computed length: " + len);
            x = 0;
            y = 0;
            z = 0;
        }
    }

    public JSONArray toJson() {
        return new JSONArray(new double[]{x, y, z});
    }

    public static Vec3 fromJson(JSONArray ja) {
        try {
            return new Vec3(ja.getDouble(0), ja.getDouble(1), ja.getDouble(2));
        } catch (Exception e) {
            return ZERO;
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof Vec3 v)
            return Double.doubleToLongBits(x) == Double.doubleToLongBits(v.x) &&
                    Double.doubleToLongBits(y) == Double.doubleToLongBits(v.y) &&
                    Double.doubleToLongBits(z) == Double.doubleToLongBits(v.z);
        return false;
    }

    @Override
    public int hashCode() {
        int result = 1;
        long tmp = Double.doubleToLongBits(x);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        tmp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        tmp = Double.doubleToLongBits(z);
        return 31 * result + (int) (tmp ^ (tmp >>> 32));
    }

    @Override
    public String toString() {
        return toJson().toString(0); // JSONException
    }

}
