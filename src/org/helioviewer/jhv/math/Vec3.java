package org.helioviewer.jhv.math;

import org.helioviewer.jhv.log.Log;

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

    public Vec3(Vec2 vec) {
        x = vec.x;
        y = vec.y;
        z = 0;
    }

    public Vec3(Vec3 vec) {
        x = vec.x;
        y = vec.y;
        z = vec.z;
    }

    public Vec3() {
        this(ZERO);
    }

    public final void set(double _x, double _y, double _z) {
        x = _x;
        y = _y;
        z = _z;
    }

    public final void add(Vec3 vec) {
        x += vec.x;
        y += vec.y;
        z += vec.z;
    }

    public static Vec3 add(Vec3 vec1, Vec3 vec2) {
        return new Vec3(vec1.x + vec2.x, vec1.y + vec2.y, vec1.z + vec2.z);
    }

    public static Vec3 subtract(Vec3 vec1, Vec3 vec2) {
        return new Vec3(vec1.x - vec2.x, vec1.y - vec2.y, vec1.z - vec2.z);
    }

    public final void multiply(double s) {
        x *= s;
        y *= s;
        z *= s;
    }

    public static Vec3 multiply(Vec3 vec1, Vec3 vec2) {
        return new Vec3(vec1.x * vec2.x, vec1.y * vec2.y, vec1.z * vec2.z);
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

    public final boolean isApproxEqual(Vec3 vec, double tolerance) {
        return Math.abs(x - vec.x) <= tolerance && Math.abs(y - vec.y) <= tolerance && Math.abs(z - vec.z) <= tolerance;
    }

    public final double length() {
        double absmax = Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z));
        if (absmax == 0.0)
            return 0.0;

        double tmpx = x / absmax;
        double tmpy = y / absmax;
        double tmpz = z / absmax;
        return absmax * Math.sqrt(tmpx * tmpx + tmpy * tmpy + tmpz * tmpz);
    }

    public final double length2() {
        double len = length();
        return len * len;
    }

    public final void normalize() {
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

    public final double[] toArray() {
        return new double[] { x, y, z };
    }

    public final Vec2 toVec2() {
        return new Vec2(x, y);
    }

    public static double[] toArray(Vec3[] vecs) {
        double[] arr = new double[vecs.length * 3];
        for (int i = 0; i < vecs.length; i++) {
            Vec3 v = vecs[i];
            arr[i * 3] = v.x;
            arr[i * 3 + 1] = v.y;
            arr[i * 3 + 2] = v.z;
        }
        return arr;
    }

    @Override
    public final boolean equals(Object o) {
        return o instanceof Vec3 && isApproxEqual((Vec3) o, 0.0);
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

    @Override
    public final String toString() {
        return "(" + x + ',' + y + ',' + z + ')';
    }

}
