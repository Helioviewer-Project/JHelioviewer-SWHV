package org.helioviewer.gl3d.scenegraph.math;

import org.helioviewer.base.logging.Log;

public class GL3DVec2d {
    /**
     * Predefined Vectors
     */
    public static final GL3DVec2d ZERO = new GL3DVec2d(0.0, 0.0);

    /**
     * Coordinates
     */
    public double x;
    public double y;

    // Constructors

    public GL3DVec2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public GL3DVec2d(GL3DVec2d vector) {
        this.x = vector.x;
        this.y = vector.y;
    }

    public GL3DVec2d() {
        this(GL3DVec2d.ZERO);
    }

    public GL3DVec2d(double[] coordinates) {
        if (coordinates == null || coordinates.length < 2) {
            throw new IllegalArgumentException("Coordinate Array must contain at least 3 dimensions");
        }
        this.x = coordinates[0];
        this.y = coordinates[1];
    }

    public GL3DVec2d add(GL3DVec2d vec) {
        this.x += vec.x;
        this.y += vec.y;
        return this;
    }

    public GL3DVec2d add(double s) {
        this.x += s;
        this.y += s;
        return this;
    }

    public static GL3DVec2d add(GL3DVec2d vec1, GL3DVec2d vec2) {
        return new GL3DVec2d(vec1.x + vec2.x, vec1.y + vec2.y);
    }

    public static GL3DVec2d add(GL3DVec2d vec1, double s) {
        return new GL3DVec2d(vec1.x + s, vec1.y + s);
    }

    public GL3DVec2d subtract(GL3DVec2d vec) {
        this.x -= vec.x;
        this.y -= vec.y;
        return this;
    }

    public GL3DVec2d subtract(double s) {
        this.x -= s;
        this.y -= s;
        return this;
    }

    public static GL3DVec2d subtract(GL3DVec2d vec1, GL3DVec2d vec2) {
        return new GL3DVec2d(vec1.x - vec2.x, vec1.y - vec2.y);
    }

    public static GL3DVec2d subtract(GL3DVec2d vec1, double s) {
        return new GL3DVec2d(vec1.x - s, vec1.y - s);
    }

    public GL3DVec2d divide(GL3DVec2d vec) {
        if (vec.x == 0.0 || vec.y == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= vec.x;
        this.y /= vec.y;
        return this;
    }

    public GL3DVec2d divide(double s) {
        if (s == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= s;
        this.y /= s;
        return this;
    }

    public static GL3DVec2d divide(GL3DVec2d vec1, GL3DVec2d vec2) {
        if (vec2.x == 0.0 || vec2.y == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new GL3DVec2d(vec1.x / vec2.x, vec1.y / vec2.y);
    }

    public static GL3DVec2d divide(GL3DVec2d vec1, double s) {
        if (s == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new GL3DVec2d(vec1.x / s, vec1.y / s);
    }

    public GL3DVec2d multiply(GL3DVec2d vec) {
        this.x *= vec.x;
        this.y *= vec.y;
        return this;
    }

    public GL3DVec2d multiply(double s) {
        this.x *= s;
        this.y *= s;
        return this;
    }

    public static GL3DVec2d multiply(GL3DVec2d vec1, GL3DVec2d vec2) {
        return new GL3DVec2d(vec1.x * vec2.x, vec1.y * vec2.y);
    }

    public static GL3DVec2d multiply(GL3DVec2d vec1, double s) {
        return new GL3DVec2d(vec1.x * s, vec1.y * s);
    }

    public double dot(GL3DVec2d vec) {
        return GL3DVec2d.dot(this, vec);
    }

    public static double dot(GL3DVec2d u, GL3DVec2d v) {
        return (u.x * v.x) + (u.y * v.y);
    }

    public GL3DVec2d negate() {
        return this.multiply(-1.0);
    }

    public static GL3DVec2d negate(GL3DVec2d vec) {
        return vec.multiply(-1.0);
    }

    public boolean isApproxEqual(GL3DVec2d vec, double tolerance) {
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

    public GL3DVec2d normalize() {
        double len = length();
        if (len == 0.0)
            return this;

        this.divide(len);

        // take shortcut, reasonably close to 1
        len = Math.sqrt(this.x * this.x + this.y * this.y);
        if (len <= 1.0)
            return this;

        // this.divide(Math.nextAfter(len, len + 1.0));
        // instead assume error is 1 ulp
        this.divide(0x1.0000000000001p+0);

        // take shortcut, reasonably close to 1
        len = Math.sqrt(this.x * this.x + this.y * this.y);
        if (len <= 1.0)
            return this;

        // can't happen / something is really messed up
        System.out.println(len);
        System.out.println(this);
        Log.error("The length of the vector is bigger than 1");
        System.exit(1);

        return ZERO;
    }

    public double[] toArray() {
        return new double[] { x, y };
    }

    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    public boolean equals(Object o) {
        if (o instanceof GL3DVec2d)
            return isApproxEqual((GL3DVec2d) o, 0.0);
        return false;
    }

    public Object clone() {
        return new GL3DVec2d(this);
    }

    public static double[] toArray(GL3DVec2d[] vecs) {
        double[] arr = new double[vecs.length * 3];
        for (int i = 0; i < vecs.length; i++) {
            GL3DVec2d v = vecs[i];
            arr[i * 2 + 0] = v.x;
            arr[i * 2 + 1] = v.y;
        }
        return arr;
    }
}
