package org.helioviewer.base.math;

import java.awt.Point;
import java.util.Locale;

/**
 * A class for two dimensional vectors with long coordinates. Instances of
 * Vector2dLong are immutable.
 * 
 * The restriction to long coordinates might lead to overflows in some
 * calculations. Consider using Vector2dDouble instead.
 * 
 * @author Ludwig Schmidt
 * 
 */
public final class Vector2dLong {

    public static final Vector2dLong NULL_VECTOR = new Vector2dLong(0, 0);
    public static final Vector2dLong MAX_VECTOR = new Vector2dLong(Long.MAX_VALUE, Long.MAX_VALUE);
    public static final Vector2dLong MIN_VECTOR = new Vector2dLong(Long.MIN_VALUE, Long.MIN_VALUE);

    private final long x;

    private final long y;

    public Vector2dLong() {
        x = 0;
        y = 0;
    }

    public Vector2dLong(final long newX, final long newY) {
        x = newX;
        y = newY;
    }

    public Vector2dLong(final Point p) {
        x = (long) p.x;
        y = (long) p.y;
    }

    public Vector2dLong(final Vector2dLong v) {
        x = v.x;
        y = v.y;
    }

    public Vector2dLong(final Vector2dInt v) {
        x = (long) v.getX();
        y = (long) v.getY();
    }

    public Vector2dLong(final Vector2dDouble v) {
        x = Math.round(v.getX());
        y = Math.round(v.getY());
    }

    public long getX() {
        return x;
    }

    public long getY() {
        return y;
    }

    public Vector2dLong getXVector() {
        return new Vector2dLong(x, 0);
    }

    public Vector2dLong getYVector() {
        return new Vector2dLong(0, y);
    }

    public Point toPoint() {
        return new Point((int) x, (int) y);
    }

    public Vector2dLong add(final Vector2dLong v) {
        return new Vector2dLong(x + v.x, y + v.y);
    }

    public static Vector2dLong add(final Vector2dLong v1, final Vector2dLong v2) {
        return new Vector2dLong(v1.x + v2.x, v1.y + v2.y);
    }

    public Vector2dLong subtract(final Vector2dLong v) {
        return new Vector2dLong(x - v.x, y - v.y);
    }

    public static Vector2dLong subtract(final Vector2dLong v1, final Vector2dLong v2) {
        return new Vector2dLong(v1.x - v2.x, v1.y - v2.y);
    }

    public Vector2dLong scale(final long s) {
        return new Vector2dLong(x * s, y * s);
    }

    public static Vector2dLong scale(final Vector2dLong v, final long s) {
        return new Vector2dLong(v.x * s, v.y * s);
    }

    public Vector2dLong scale(final double d) {
        return new Vector2dLong(Math.round(x * d), Math.round(y * d));
    }

    public static Vector2dLong scale(final Vector2dLong v, final double d) {
        return new Vector2dLong(Math.round(v.x * d), Math.round(v.y * d));
    }

    public Vector2dLong negate() {
        return new Vector2dLong(-x, -y);
    }

    public static Vector2dLong negate(final Vector2dLong v) {
        return new Vector2dLong(-v.x, -v.y);
    }

    public Vector2dLong negateX() {
        return new Vector2dLong(-x, y);
    }

    public static Vector2dLong negateX(final Vector2dLong v) {
        return new Vector2dLong(-v.x, v.y);
    }

    public Vector2dLong negateY() {
        return new Vector2dLong(x, -y);
    }

    public static Vector2dLong negateY(final Vector2dLong v) {
        return new Vector2dLong(v.x, -v.y);
    }

    public Vector2dLong crop(Vector2dLong min, Vector2dLong max) {
        return new Vector2dLong(Math.min(max.x, Math.max(min.x, x)), Math.min(max.y, Math.max(min.y, y)));
    }

    public static Vector2dLong crop(Vector2dLong v, Vector2dLong min, Vector2dLong max) {
        return new Vector2dLong(Math.min(max.x, Math.max(min.x, v.x)), Math.min(max.x, Math.max(min.x, v.y)));
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public long lengthSq() {
        return x * x + y * y;
    }

    public Vector2dLong normalize() {
        double length = Math.sqrt(x * x + y * y);
        return new Vector2dLong(Math.round(x / length), Math.round(y / length));
    }

    public static Vector2dLong normalize(final Vector2dLong v) {
        double length = Math.sqrt(v.x * v.x + v.y * v.y);
        return new Vector2dLong(Math.round(v.x / length), Math.round(v.y / length));
    }

    public static long dot(final Vector2dLong v1, final Vector2dLong v2) {
        return v1.x * v2.x + v1.y * v2.y;
    }

    public static double angle(final Vector2dLong v1, final Vector2dLong v2) {
        return Math.acos((v1.x * v2.x + v1.y * v2.y) / (Math.sqrt(v1.x * v1.x + v1.y * v1.y) * Math.sqrt(v2.x * v2.x + v2.y * v2.y)));
    }

    public Vector2dLong absolute() {
        return new Vector2dLong(Math.abs(x), Math.abs(y));
    }

    public static Vector2dLong absolute(final Vector2dLong v) {
        return new Vector2dLong(Math.abs(v.x), Math.abs(v.y));
    }

    public boolean equals(final Object o) {
        if (!(o instanceof Vector2dLong)) {
            return false;
        }
        Vector2dLong v = (Vector2dLong) o;
        return v.x == x && v.y == y;
    }

    /**
     * The multiplier used for the hash code computation.
     */
    private static final int HASH_CODE_MULTIPLIER = 31;

    /**
     * The initial value used for the hash code computation.
     */
    private static final int HASH_CODE_INITIAL_VALUE = 17;

    /**
     * The number of bits in an integer, used to compute a has value of the
     * double values.
     */
    private static final int HASH_CODE_INT_BITS = 32;

    public int hashCode() {
        return HASH_CODE_INITIAL_VALUE * HASH_CODE_MULTIPLIER * HASH_CODE_MULTIPLIER + HASH_CODE_MULTIPLIER * (int) (x ^ (x >>> HASH_CODE_INT_BITS)) + (int) (y ^ (y >>> HASH_CODE_INT_BITS));
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "(%l,%l)", x, y);
    }
}
