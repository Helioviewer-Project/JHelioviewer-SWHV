package org.helioviewer.base.math;

import java.awt.Point;

import org.helioviewer.gl3d.math.GL3DVec2d;

/**
 * A class for two dimensional vectors with integer coordinates. Instances of
 * Vector2dInt are immutable.
 *
 * @author Ludwig Schmidt
 *
 */
public final class Vector2dInt {

    public static final Vector2dInt NULL_VECTOR = new Vector2dInt(0, 0);
    public static final Vector2dInt MAX_VECTOR = new Vector2dInt(Integer.MAX_VALUE, Integer.MAX_VALUE);
    public static final Vector2dInt MIN_VECTOR = new Vector2dInt(Integer.MIN_VALUE, Integer.MIN_VALUE);

    private final int x;

    private final int y;

    public Vector2dInt() {
        x = 0;
        y = 0;
    }

    public Vector2dInt(final int newX, final int newY) {
        x = newX;
        y = newY;
    }

    public Vector2dInt(final Point p) {
        x = p.x;
        y = p.y;
    }

    public Vector2dInt(final Vector2dInt v) {
        x = v.x;
        y = v.y;
    }

    public Vector2dInt(final GL3DVec2d v) {
        x = (int) Math.round(v.x);
        y = (int) Math.round(v.y);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Vector2dInt getXVector() {
        return new Vector2dInt(x, 0);
    }

    public Vector2dInt getYVector() {
        return new Vector2dInt(0, y);
    }

    public Point toPoint() {
        return new Point(x, y);
    }

    public Vector2dInt add(final Vector2dInt v) {
        return new Vector2dInt(x + v.x, y + v.y);
    }

    public static Vector2dInt add(final Vector2dInt v1, final Vector2dInt v2) {
        return new Vector2dInt(v1.x + v2.x, v1.y + v2.y);
    }

    public Vector2dInt subtract(final Vector2dInt v) {
        return new Vector2dInt(x - v.x, y - v.y);
    }

    public static Vector2dInt subtract(final Vector2dInt v1, final Vector2dInt v2) {
        return new Vector2dInt(v1.x - v2.x, v1.y - v2.y);
    }

    public Vector2dInt scale(final int s) {
        return new Vector2dInt(x * s, y * s);
    }

    public static Vector2dInt scale(final Vector2dInt v, final int s) {
        return new Vector2dInt(v.x * s, v.y * s);
    }

    public Vector2dInt scale(final double d) {
        return new Vector2dInt((int) Math.round(x * d), (int) Math.round(y * d));
    }

    public static Vector2dInt scale(final Vector2dInt v, final double d) {
        return new Vector2dInt((int) Math.round(v.x * d), (int) Math.round(v.y * d));
    }

    public Vector2dInt negate() {
        return new Vector2dInt(-x, -y);
    }

    public static Vector2dInt negate(final Vector2dInt v) {
        return new Vector2dInt(-v.x, -v.y);
    }

    public Vector2dInt negateX() {
        return new Vector2dInt(-x, y);
    }

    public static Vector2dInt negateX(final Vector2dInt v) {
        return new Vector2dInt(-v.x, v.y);
    }

    public Vector2dInt negateY() {
        return new Vector2dInt(x, -y);
    }

    public static Vector2dInt negateY(final Vector2dInt v) {
        return new Vector2dInt(v.x, -v.y);
    }

    public Vector2dInt crop(Vector2dInt min, Vector2dInt max) {
        return new Vector2dInt(Math.min(max.x, Math.max(min.x, x)), Math.min(max.y, Math.max(min.y, y)));
    }

    public static Vector2dInt crop(Vector2dInt v, Vector2dInt min, Vector2dInt max) {
        return new Vector2dInt(Math.min(max.x, Math.max(min.x, v.x)), Math.min(max.x, Math.max(min.x, v.y)));
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public int lengthSq() {
        return x * x + y * y;
    }

    public Vector2dInt normalize() {
        double length = Math.sqrt(x * x + y * y);
        return new Vector2dInt((int) Math.round(x / length), (int) Math.round(y / length));
    }

    public static Vector2dInt normalize(final Vector2dInt v) {
        double length = Math.sqrt(v.x * v.x + v.y * v.y);
        return new Vector2dInt((int) Math.round(v.x / length), (int) Math.round(v.y / length));
    }

    public static int dot(final Vector2dInt v1, final Vector2dInt v2) {
        return v1.x * v2.x + v1.y * v2.y;
    }

    public static double angle(final Vector2dInt v1, final Vector2dInt v2) {
        return Math.acos((v1.x * v2.x + v1.y * v2.y) / ((Math.sqrt(v1.x * v1.x + v1.y * v1.y)) * (Math.sqrt(v2.x * v2.x + v2.y * v2.y))));
    }

    public Vector2dInt absolute() {
        return new Vector2dInt(Math.abs(x), Math.abs(y));
    }

    public static Vector2dInt absolute(final Vector2dInt v) {
        return new Vector2dInt(Math.abs(v.x), Math.abs(v.y));
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof Vector2dInt)) {
            return false;
        }
        Vector2dInt v = (Vector2dInt) o;
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

    @Override
    public int hashCode() {
        return HASH_CODE_INITIAL_VALUE * HASH_CODE_MULTIPLIER * HASH_CODE_MULTIPLIER + HASH_CODE_MULTIPLIER * x + y;
    }

    @Override
    public String toString() {
        return String.format("(%d,%d)", x, y);
    }

}
