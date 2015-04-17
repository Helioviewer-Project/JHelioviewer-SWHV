package org.helioviewer.base.math;

import java.awt.Point;
import java.util.Locale;

/**
 * A class for two dimensional vectors with double coordinates. Instances of
 * Vector2dDouble are immutable.
 *
 * @author Ludwig Schmidt
 *
 */

public final class Vector2dDouble {

    public static final Vector2dDouble NULL_VECTOR = new Vector2dDouble(0, 0);
    public static final Vector2dDouble NEGATIVE_INFINITY_VECTOR = new Vector2dDouble(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    public static final Vector2dDouble POSITIVE_INFINITY_VECTOR = new Vector2dDouble(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    private final double x;

    private final double y;

    public Vector2dDouble() {
        x = 0.0;
        y = 0.0;
    }

    public Vector2dDouble(final double newX, final double newY) {
        x = newX;
        y = newY;
    }

    public Vector2dDouble(final Point p) {
        x = p.x;
        y = p.y;
    }

    public Vector2dDouble(final Vector2dDouble v) {
        x = v.x;
        y = v.y;
    }

    public Vector2dDouble(final Vector2dInt v) {
        x = v.getX();
        y = v.getY();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Vector2dDouble getXVector() {
        return new Vector2dDouble(x, 0.0);
    }

    public Vector2dDouble getYVector() {
        return new Vector2dDouble(0.0, y);
    }

    public Point toPoint() {
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    public Vector2dDouble add(final Vector2dDouble v) {
        return new Vector2dDouble(x + v.x, y + v.y);
    }

    public static Vector2dDouble add(final Vector2dDouble v1, final Vector2dDouble v2) {
        return new Vector2dDouble(v1.x + v2.x, v1.y + v2.y);
    }

    public Vector2dDouble subtract(final Vector2dDouble v) {
        return new Vector2dDouble(x - v.x, y - v.y);
    }

    public static Vector2dDouble subtract(final Vector2dDouble v1, final Vector2dDouble v2) {
        return new Vector2dDouble(v1.x - v2.x, v1.y - v2.y);
    }

    public Vector2dDouble scale(final double d) {
        return new Vector2dDouble(x * d, y * d);
    }

    public Vector2dDouble scale(final Vector2dDouble v) {
        return new Vector2dDouble(x * v.x, y * v.y);
    }

    public Vector2dDouble invertedScale(final Vector2dDouble v) {
        return new Vector2dDouble(x / v.x, y / v.y);
    }

    public static Vector2dDouble scale(final Vector2dDouble v, final double d) {
        return new Vector2dDouble(v.x * d, v.y * d);
    }

    public Vector2dDouble negate() {
        return new Vector2dDouble(-x, -y);
    }

    public static Vector2dDouble negate(final Vector2dDouble v) {
        return new Vector2dDouble(-v.x, -v.y);
    }

    public Vector2dDouble negateX() {
        return new Vector2dDouble(-x, y);
    }

    public static Vector2dDouble negateX(final Vector2dDouble v) {
        return new Vector2dDouble(-v.x, v.y);
    }

    public Vector2dDouble negateY() {
        return new Vector2dDouble(x, -y);
    }

    public static Vector2dDouble negateY(final Vector2dDouble v) {
        return new Vector2dDouble(v.x, -v.y);
    }

    public Vector2dDouble crop(Vector2dDouble min, Vector2dDouble max) {
        return new Vector2dDouble(Math.min(max.x, Math.max(min.x, x)), Math.min(max.y, Math.max(min.y, y)));
    }

    public static Vector2dDouble crop(Vector2dDouble v, Vector2dDouble min, Vector2dDouble max) {
        return new Vector2dDouble(Math.min(max.x, Math.max(min.x, v.x)), Math.min(max.x, Math.max(min.x, v.y)));
    }

    public Vector2dDouble componentMin(final Vector2dDouble v) {
        return new Vector2dDouble(Math.min(x, v.x), Math.min(y, v.y));
    }

    public static Vector2dDouble componentMin(final Vector2dDouble v1, final Vector2dDouble v2) {
        return new Vector2dDouble(Math.min(v1.x, v2.x), Math.min(v1.y, v2.y));
    }

    public Vector2dDouble componentMax(final Vector2dDouble v) {
        return new Vector2dDouble(Math.max(x, v.x), Math.max(y, v.y));
    }

    public static Vector2dDouble componentMax(final Vector2dDouble v1, final Vector2dDouble v2) {
        return new Vector2dDouble(Math.max(v1.x, v2.x), Math.max(v1.y, v2.y));
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public double lengthSq() {
        return x * x + y * y;
    }

    public Vector2dDouble normalize() {
        double length = Math.sqrt(x * x + y * y);
        return new Vector2dDouble(x / length, y / length);
    }

    public static Vector2dDouble normalize(final Vector2dDouble v) {
        double length = Math.sqrt(v.x * v.x + v.y * v.y);
        return new Vector2dDouble(v.x / length, v.y / length);
    }

    public static double dot(final Vector2dDouble v1, final Vector2dDouble v2) {
        return v1.x * v2.x + v1.y * v2.y;
    }

    public static double angle(final Vector2dDouble v1, final Vector2dDouble v2) {
        return Math.acos((v1.x * v2.x + v1.y * v2.y) / ((Math.sqrt(v1.x * v1.x + v1.y * v1.y)) * (Math.sqrt(v2.x * v2.x + v2.y * v2.y))));
    }

    public Vector2dDouble absolute() {
        return new Vector2dDouble(Math.abs(x), Math.abs(y));
    }

    public static Vector2dDouble absolute(final Vector2dDouble v) {
        return new Vector2dDouble(Math.abs(v.x), Math.abs(v.y));
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof Vector2dDouble)) {
            return false;
        }
        Vector2dDouble v = (Vector2dDouble) o;

        return Double.compare(x, v.x) == 0 && Double.compare(y, v.y) == 0;
    }

    public boolean epsilonEquals(final Vector2dDouble v, final double epsilon) {
        return Math.abs(x - v.x) < epsilon && Math.abs(y - v.y) < epsilon;
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

    @Override
    public int hashCode() {
        long xBits = Double.doubleToLongBits(x);
        long yBits = Double.doubleToLongBits(y);
        return HASH_CODE_INITIAL_VALUE * HASH_CODE_MULTIPLIER * HASH_CODE_MULTIPLIER + HASH_CODE_MULTIPLIER * (int) (xBits ^ (xBits >>> HASH_CODE_INT_BITS)) + (int) (yBits ^ (yBits >>> HASH_CODE_INT_BITS));
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "(%f,%f)", x, y);
    }
}
