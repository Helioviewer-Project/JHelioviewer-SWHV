package org.helioviewer.gl3d.scenegraph.math;

public class GL3DVec2d {
    /**
     * Predefined Vectors
     */
    public static final GL3DVec2d ZERO = new GL3DVec2d(0f, 0f);

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
        if (vec.x == 0f || vec.y == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= vec.x;
        this.y /= vec.y;
        return this;
    }

    public GL3DVec2d divide(double s) {
        if (s == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= s;
        this.y /= s;
        return this;
    }

    public static GL3DVec2d divide(GL3DVec2d vec1, GL3DVec2d vec2) {
        if (vec2.x == 0f || vec2.y == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new GL3DVec2d(vec1.x / vec2.x, vec1.y / vec2.y);
    }

    public static GL3DVec2d divide(GL3DVec2d vec1, double s) {
        if (s == 0f)
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
        return this.multiply(-1f);
    }

    public static GL3DVec2d negate(GL3DVec2d vec) {
        return vec.multiply(-1f);
    }

    public boolean isApproxEqual(GL3DVec2d vec, double tolerance) {
        return Math.abs(this.x - vec.x) <= tolerance && Math.abs(this.y - vec.y) <= tolerance;
    }

    public double length() {
        return (double) Math.sqrt(this.x * this.x + this.y * this.y);
    }

    public double length2() {
        return (this.x * this.x + this.y * this.y);
    }

    public GL3DVec2d normalize() {
        return this.divide(length());
    }

    public double[] toArray() {
        return new double[] { x, y };
    }

    public boolean equals(Object o) {
        if (o instanceof GL3DVec2d)
            return isApproxEqual((GL3DVec2d) o, 0.0f);
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
