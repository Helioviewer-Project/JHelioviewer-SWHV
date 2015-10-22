package org.helioviewer.jhv.base.math;

import org.helioviewer.jhv.base.logging.Log;

public class Vec2d {
    /**
     * Predefined Vectors
     */
    public static final Vec2d ZERO = new Vec2d(0.0, 0.0);
    public static final Vec2d NEGATIVE_INFINITY_VECTOR = new Vec2d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    public static final Vec2d POSITIVE_INFINITY_VECTOR = new Vec2d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    /**
     * Coordinates
     */
    public double x;
    public double y;

    // Constructors

    public Vec2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2d(Vec2d vector) {
        this.x = vector.x;
        this.y = vector.y;
    }

    public Vec2d() {
        this(Vec2d.ZERO);
    }

    public Vec2d(double[] coordinates) {
        if (coordinates == null || coordinates.length < 2) {
            throw new IllegalArgumentException("Coordinate Array must contain at least 3 dimensions");
        }
        this.x = coordinates[0];
        this.y = coordinates[1];
    }

    public Vec2d(Vector2dInt newLowerLeftCorner) {
        this(newLowerLeftCorner.getX(), newLowerLeftCorner.getY());
    }

    public void add(Vec2d vec) {
        this.x += vec.x;
        this.y += vec.y;
    }

    public void add(double s) {
        this.x += s;
        this.y += s;
    }

    public static Vec2d add(Vec2d vec1, Vec2d vec2) {
        return new Vec2d(vec1.x + vec2.x, vec1.y + vec2.y);
    }

    public static Vec2d add(Vec2d vec1, double s) {
        return new Vec2d(vec1.x + s, vec1.y + s);
    }

    public void subtract(Vec2d vec) {
        this.x -= vec.x;
        this.y -= vec.y;
    }

    public void subtract(double s) {
        this.x -= s;
        this.y -= s;
    }

    public static Vec2d subtract(Vec2d vec1, Vec2d vec2) {
        return new Vec2d(vec1.x - vec2.x, vec1.y - vec2.y);
    }

    public static Vec2d subtract(Vec2d vec1, double s) {
        return new Vec2d(vec1.x - s, vec1.y - s);
    }

    public void divide(Vec2d vec) {
        if (vec.x == 0.0 || vec.y == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= vec.x;
        this.y /= vec.y;
    }

    public void divide(double s) {
        if (s == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= s;
        this.y /= s;
    }

    public static Vec2d divide(Vec2d vec1, Vec2d vec2) {
        if (vec2.x == 0.0 || vec2.y == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new Vec2d(vec1.x / vec2.x, vec1.y / vec2.y);
    }

    public static Vec2d divide(Vec2d vec1, double s) {
        if (s == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new Vec2d(vec1.x / s, vec1.y / s);
    }

    public void multiply(Vec2d vec) {
        this.x *= vec.x;
        this.y *= vec.y;
    }

    public void multiply(double s) {
        this.x *= s;
        this.y *= s;
    }

    public static Vec2d multiply(Vec2d vec1, Vec2d vec2) {
        return new Vec2d(vec1.x * vec2.x, vec1.y * vec2.y);
    }

    public static Vec2d multiply(Vec2d vec1, double s) {
        return new Vec2d(vec1.x * s, vec1.y * s);
    }

    public double dot(Vec2d vec) {
        return Vec2d.dot(this, vec);
    }

    public static double dot(Vec2d u, Vec2d v) {
        return (u.x * v.x) + (u.y * v.y);
    }

    public void negate() {
        this.x = -this.x;
        this.y = -this.y;
    }

    public static Vec2d negate(Vec2d vec) {
        Vec2d newVec = vec.copy();
        newVec.negate();
        return newVec;
    }

    private Vec2d copy() {
        return new Vec2d(this.x, this.y);
    }

    public boolean isApproxEqual(Vec2d vec, double tolerance) {
        return Math.abs(this.x - vec.x) <= tolerance && Math.abs(this.y - vec.y) <= tolerance;
    }

    public double length() {
        double absmax = Math.max(Math.abs(this.x), Math.abs(this.y));
        if (absmax == 0.0)
            return 0.0;

        double tmpx = this.x / absmax;
        double tmpy = this.y / absmax;
        return absmax * Math.sqrt(tmpx * tmpx + tmpy * tmpy);
    }

    public double length2() {
        double len = length();
        return len * len;
    }

    public void normalize() {
        double len = length();
        if (len == 0.0)
            return;

        this.divide(len);

        // take shortcut, reasonably close to 1
        len = Math.sqrt(this.x * this.x + this.y * this.y);
        if (len <= 1.0)
            return;

        // errors up to 2ulp found in testing
        this.divide(Math.nextAfter(len, len + 1.0));

        // take shortcut, reasonably close to 1
        len = Math.sqrt(this.x * this.x + this.y * this.y);
        if (len <= 1.0)
            return;

        // can't happen / something is really messed up
        System.out.println(len);
        System.out.println(this);
        Log.error("The length of the vector is bigger than 1");
        System.exit(1);

        this.x = Double.NaN;
        this.y = Double.NaN;
    }

    public double[] toArray() {
        return new double[] { x, y };
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Vec2d)
            return isApproxEqual((Vec2d) o, 0.0);
        return false;
    }

    public static double[] toArray(Vec2d[] vecs) {
        double[] arr = new double[vecs.length * 3];
        for (int i = 0; i < vecs.length; i++) {
            Vec2d v = vecs[i];
            arr[i * 2 + 0] = v.x;
            arr[i * 2 + 1] = v.y;
        }
        return arr;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    public static Vec2d scale(Vec2d vec, double scale) {
        return new Vec2d(vec.x * scale, vec.y * scale);
    }

    public static Vec2d scale(Vec2d vec, Vec2d scale) {
        return new Vec2d(vec.x * scale.x, vec.y * scale.y);
    }

    public static Vec2d invertedScale(Vec2d vec, Vec2d scale) {
        return new Vec2d(vec.x / scale.x, vec.y / scale.y);

    }

    public Vec2d getYVector() {
        return new Vec2d(0., this.y);
    }

    public Vec2d getXVector() {
        return new Vec2d(this.x, 0.);
    }

    public static Vec2d crop(Vec2d v, Vec2d min, Vec2d max) {
        return new Vec2d(Math.min(max.x, Math.max(min.x, v.x)), Math.min(max.x, Math.max(min.x, v.y)));
    }

    public static Vec2d componentMin(final Vec2d v1, final Vec2d v2) {
        return new Vec2d(Math.min(v1.x, v2.x), Math.min(v1.y, v2.y));
    }

    public static Vec2d componentMax(final Vec2d v1, final Vec2d v2) {
        return new Vec2d(Math.max(v1.x, v2.x), Math.max(v1.y, v2.y));
    }
}
