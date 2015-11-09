package org.helioviewer.jhv.base.math;

import org.helioviewer.jhv.base.logging.Log;

public class Vec2 {
    /**
     * Predefined Vectors
     */
    public static final Vec2 ZERO = new Vec2(0.0, 0.0);
    public static final Vec2 NEGATIVE_INFINITY_VECTOR = new Vec2(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    public static final Vec2 POSITIVE_INFINITY_VECTOR = new Vec2(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    /**
     * Coordinates
     */
    public double x;
    public double y;

    // Constructors

    public Vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2(Vec2 vector) {
        this.x = vector.x;
        this.y = vector.y;
    }

    public Vec2() {
        this(Vec2.ZERO);
    }

    public Vec2(double[] coordinates) {
        if (coordinates == null || coordinates.length < 2) {
            throw new IllegalArgumentException("Coordinate Array must contain at least 3 dimensions");
        }
        this.x = coordinates[0];
        this.y = coordinates[1];
    }

    public void add(Vec2 vec) {
        this.x += vec.x;
        this.y += vec.y;
    }

    public void add(double s) {
        this.x += s;
        this.y += s;
    }

    public static Vec2 add(Vec2 vec1, Vec2 vec2) {
        return new Vec2(vec1.x + vec2.x, vec1.y + vec2.y);
    }

    public static Vec2 add(Vec2 vec1, double s) {
        return new Vec2(vec1.x + s, vec1.y + s);
    }

    public void subtract(Vec2 vec) {
        this.x -= vec.x;
        this.y -= vec.y;
    }

    public void subtract(double s) {
        this.x -= s;
        this.y -= s;
    }

    public static Vec2 subtract(Vec2 vec1, Vec2 vec2) {
        return new Vec2(vec1.x - vec2.x, vec1.y - vec2.y);
    }

    public static Vec2 subtract(Vec2 vec1, double s) {
        return new Vec2(vec1.x - s, vec1.y - s);
    }

    public void divide(Vec2 vec) {
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

    public static Vec2 divide(Vec2 vec1, Vec2 vec2) {
        if (vec2.x == 0.0 || vec2.y == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new Vec2(vec1.x / vec2.x, vec1.y / vec2.y);
    }

    public static Vec2 divide(Vec2 vec1, double s) {
        if (s == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new Vec2(vec1.x / s, vec1.y / s);
    }

    public void multiply(Vec2 vec) {
        this.x *= vec.x;
        this.y *= vec.y;
    }

    public void multiply(double s) {
        this.x *= s;
        this.y *= s;
    }

    public static Vec2 multiply(Vec2 vec1, Vec2 vec2) {
        return new Vec2(vec1.x * vec2.x, vec1.y * vec2.y);
    }

    public static Vec2 multiply(Vec2 vec1, double s) {
        return new Vec2(vec1.x * s, vec1.y * s);
    }

    public double dot(Vec2 vec) {
        return Vec2.dot(this, vec);
    }

    public static double dot(Vec2 u, Vec2 v) {
        return (u.x * v.x) + (u.y * v.y);
    }

    public void negate() {
        this.x = -this.x;
        this.y = -this.y;
    }

    public static Vec2 negate(Vec2 vec) {
        Vec2 newVec = vec.copy();
        newVec.negate();
        return newVec;
    }

    public Vec2 copy() {
        return new Vec2(this);
    }

    public boolean isApproxEqual(Vec2 vec, double tolerance) {
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

    public static double[] toArray(Vec2[] vecs) {
        double[] arr = new double[vecs.length * 3];
        for (int i = 0; i < vecs.length; i++) {
            Vec2 v = vecs[i];
            arr[i * 2 + 0] = v.x;
            arr[i * 2 + 1] = v.y;
        }
        return arr;
    }

    public static Vec2 scale(Vec2 vec, double scale) {
        return new Vec2(vec.x * scale, vec.y * scale);
    }

    public static Vec2 scale(Vec2 vec, Vec2 scale) {
        return new Vec2(vec.x * scale.x, vec.y * scale.y);
    }

    public static Vec2 invertedScale(Vec2 vec, Vec2 scale) {
        return new Vec2(vec.x / scale.x, vec.y / scale.y);
    }

    public Vec2 getYVector() {
        return new Vec2(0., this.y);
    }

    public Vec2 getXVector() {
        return new Vec2(this.x, 0.);
    }

    public static Vec2 crop(Vec2 v, Vec2 min, Vec2 max) {
        return new Vec2(Math.min(max.x, Math.max(min.x, v.x)), Math.min(max.x, Math.max(min.x, v.y)));
    }

    public static Vec2 componentMin(final Vec2 v1, final Vec2 v2) {
        return new Vec2(Math.min(v1.x, v2.x), Math.min(v1.y, v2.y));
    }

    public static Vec2 componentMax(final Vec2 v1, final Vec2 v2) {
        return new Vec2(Math.max(v1.x, v2.x), Math.max(v1.y, v2.y));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Vec2)
            return isApproxEqual((Vec2) o, 0.0);
        return false;
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

}
