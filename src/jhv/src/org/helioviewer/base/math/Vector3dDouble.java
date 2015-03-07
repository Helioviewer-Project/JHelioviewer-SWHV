package org.helioviewer.base.math;

import java.util.Locale;

/**
 * A class for three dimensional vectors with double coordinates. Instances of
 * Vector3dDouble are immutable.
 * 
 * @author Ludwig Schmidt
 * @author Malte Nuhn
 * 
 */

public final class Vector3dDouble {

    public static final Vector3dDouble NULL_VECTOR = new Vector3dDouble(0, 0, 0);
    public static final Vector3dDouble NEGATIVE_INFINITY_VECTOR = new Vector3dDouble(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    public static final Vector3dDouble POSITIVE_INFINITY_VECTOR = new Vector3dDouble(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    private final double x;
    private final double y;
    private final double z;

    public Vector3dDouble() {
        x = 0.0;
        y = 0.0;
        z = 0.0;
    }

    public Vector3dDouble(final double newX, final double newY, final double newZ) {
        x = newX;
        y = newY;
        z = newZ;
    }

    public Vector3dDouble(final Vector3dDouble v) {
        x = v.x;
        y = v.y;
        z = v.z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Vector3dDouble getXVector() {
        return new Vector3dDouble(x, 0.0, 0.0);
    }

    public Vector3dDouble getYVector() {
        return new Vector3dDouble(0.0, y, 0.0);
    }

    public Vector3dDouble getZVector() {
        return new Vector3dDouble(0.0, 0.0, z);
    }

    public Vector3dDouble add(final Vector3dDouble v) {
        return new Vector3dDouble(x + v.x, y + v.y, z + v.z);
    }

    public static Vector3dDouble add(final Vector3dDouble v1, final Vector3dDouble v2) {
        return new Vector3dDouble(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z);
    }

    public Vector3dDouble subtract(final Vector3dDouble v) {
        return new Vector3dDouble(x - v.x, y - v.y, z - v.z);
    }

    public static Vector3dDouble subtract(final Vector3dDouble v1, final Vector3dDouble v2) {
        return new Vector3dDouble(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
    }

    public Vector3dDouble scale(final double d) {
        return new Vector3dDouble(x * d, y * d, z * d);
    }

    public Vector3dDouble scale(final Vector3dDouble v) {
        return new Vector3dDouble(x * v.x, y * v.y, z * v.z);
    }

    public Vector3dDouble invertedScale(final Vector3dDouble v) {
        return new Vector3dDouble(x / v.x, y / v.y, z / v.z);
    }

    public static Vector3dDouble scale(final Vector3dDouble v, final double d) {
        return new Vector3dDouble(v.x * d, v.y * d, v.z * d);
    }

    public Vector3dDouble negate() {
        return new Vector3dDouble(-x, -y, -z);
    }

    public static Vector3dDouble negate(final Vector3dDouble v) {
        return new Vector3dDouble(-v.x, -v.y, -v.z);
    }

    public Vector3dDouble negateX() {
        return new Vector3dDouble(-x, y, z);
    }

    public static Vector3dDouble negateX(final Vector3dDouble v) {
        return new Vector3dDouble(-v.x, v.y, v.z);
    }

    public Vector3dDouble negateY() {
        return new Vector3dDouble(x, -y, z);
    }

    public static Vector3dDouble negateY(final Vector3dDouble v) {
        return new Vector3dDouble(v.x, -v.y, v.z);
    }

    public Vector3dDouble negateZ() {
        return new Vector3dDouble(x, y, -z);
    }

    public static Vector3dDouble negateZ(final Vector3dDouble v) {
        return new Vector3dDouble(v.x, v.y, -v.z);
    }

    public Vector3dDouble crop(Vector3dDouble min, Vector3dDouble max) {
        return new Vector3dDouble(Math.min(max.x, Math.max(min.x, x)), Math.min(max.y, Math.max(min.y, y)), Math.min(max.z, Math.max(min.z, z)));
    }

    public static Vector3dDouble crop(Vector3dDouble v, Vector3dDouble min, Vector3dDouble max) {
        return new Vector3dDouble(Math.min(max.x, Math.max(min.x, v.x)), Math.min(max.y, Math.max(min.y, v.y)), Math.min(max.z, Math.max(min.z, v.z)));
    }

    public Vector3dDouble componentMin(final Vector3dDouble v) {
        return new Vector3dDouble(Math.min(x, v.x), Math.min(y, v.y), Math.min(z, v.z));
    }

    public static Vector3dDouble componentMin(final Vector3dDouble v1, final Vector3dDouble v2) {
        return new Vector3dDouble(Math.min(v1.x, v2.x), Math.min(v1.y, v2.y), Math.min(v1.z, v2.z));
    }

    public Vector3dDouble componentMax(final Vector3dDouble v) {
        return new Vector3dDouble(Math.max(x, v.x), Math.max(y, v.y), Math.max(z, v.z));
    }

    public static Vector3dDouble componentMax(final Vector3dDouble v1, final Vector3dDouble v2) {
        return new Vector3dDouble(Math.max(v1.x, v2.x), Math.max(v1.y, v2.y), Math.max(v1.z, v2.z));
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double lengthSq() {
        return x * x + y * y + z * z;
    }

    public Vector3dDouble normalize() {
        double length = this.length();
        return new Vector3dDouble(x / length, y / length, z / length);
    }

    public static Vector3dDouble normalize(final Vector3dDouble v) {
        double length = v.length();
        return new Vector3dDouble(v.x / length, v.y / length, v.z / length);
    }

    public static double dot(final Vector3dDouble v1, final Vector3dDouble v2) {
        return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
    }

    public static Vector3dDouble cross(final Vector3dDouble v1, final Vector3dDouble v2) {
        double x1 = v1.y * v2.z - v1.z * v2.y;
        double x2 = v1.z * v2.x - v1.x * v2.z;
        double x3 = v1.x * v2.y - v1.y * v2.x;

        return new Vector3dDouble(x1, x2, x3);
    }

    public Vector3dDouble absolute() {
        return new Vector3dDouble(Math.abs(x), Math.abs(y), Math.abs(z));
    }

    public static Vector3dDouble absolute(final Vector3dDouble v) {
        return new Vector3dDouble(Math.abs(v.x), Math.abs(v.y), Math.abs(v.z));
    }

    public boolean equals(final Object o) {
        if (!(o instanceof Vector3dDouble)) {
            return false;
        }
        Vector3dDouble v = (Vector3dDouble) o;

        return Double.compare(x, v.x) == 0 && Double.compare(y, v.y) == 0 && Double.compare(z, v.z) == 0;
    }

    public boolean epsilonEquals(final Vector3dDouble v, final double epsilon) {
        return Math.abs(x - v.x) < epsilon && Math.abs(y - v.y) < epsilon && Math.abs(z - v.z) < epsilon;
    }

    public int hashCode() {
        return this.toString().hashCode(); // simple but good
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "(%.2f,%.2f,%.2f)", x, y, z);
    }
}
