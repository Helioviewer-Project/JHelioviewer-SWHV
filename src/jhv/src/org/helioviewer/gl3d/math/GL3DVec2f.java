package org.helioviewer.gl3d.math;

public class GL3DVec2f {
    /**
     * Predefined Vectors
     */
    public static final GL3DVec2f ZERO = new GL3DVec2f(0f, 0f);

    /**
     * Coordinates
     */
    public float x;
    public float y;

    // Constructors

    public GL3DVec2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public GL3DVec2f(GL3DVec2f vector) {
        this.x = vector.x;
        this.y = vector.y;
    }

    public GL3DVec2f() {
        this(GL3DVec2f.ZERO);
    }

    public GL3DVec2f(float[] coordinates) {
        if (coordinates == null || coordinates.length < 2) {
            throw new IllegalArgumentException("Coordinate Array must contain at least 3 dimensions");
        }
        this.x = coordinates[0];
        this.y = coordinates[1];
    }

    public void add(GL3DVec2f vec) {
        this.x += vec.x;
        this.y += vec.y;
    }

    public void add(float s) {
        this.x += s;
        this.y += s;
    }

    public static GL3DVec2f add(GL3DVec2f vec1, GL3DVec2f vec2) {
        return new GL3DVec2f(vec1.x + vec2.x, vec1.y + vec2.y);
    }

    public static GL3DVec2f add(GL3DVec2f vec1, float s) {
        return new GL3DVec2f(vec1.x + s, vec1.y + s);
    }

    public void subtract(GL3DVec2f vec) {
        this.x -= vec.x;
        this.y -= vec.y;
    }

    public void subtract(float s) {
        this.x -= s;
        this.y -= s;
    }

    public static GL3DVec2f subtract(GL3DVec2f vec1, GL3DVec2f vec2) {
        return new GL3DVec2f(vec1.x - vec2.x, vec1.y - vec2.y);
    }

    public static GL3DVec2f subtract(GL3DVec2f vec1, float s) {
        return new GL3DVec2f(vec1.x - s, vec1.y - s);
    }

    public void divide(GL3DVec2f vec) {
        if (vec.x == 0f || vec.y == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= vec.x;
        this.y /= vec.y;
    }

    public void divide(float s) {
        if (s == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= s;
        this.y /= s;
    }

    public static GL3DVec2f divide(GL3DVec2f vec1, GL3DVec2f vec2) {
        if (vec2.x == 0f || vec2.y == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new GL3DVec2f(vec1.x / vec2.x, vec1.y / vec2.y);
    }

    public static GL3DVec2f divide(GL3DVec2f vec1, float s) {
        if (s == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new GL3DVec2f(vec1.x / s, vec1.y / s);
    }

    public void multiply(GL3DVec2f vec) {
        this.x *= vec.x;
        this.y *= vec.y;
    }

    public void multiply(float s) {
        this.x *= s;
        this.y *= s;
    }

    public static GL3DVec2f multiply(GL3DVec2f vec1, GL3DVec2f vec2) {
        return new GL3DVec2f(vec1.x * vec2.x, vec1.y * vec2.y);
    }

    public static GL3DVec2f multiply(GL3DVec2f vec1, float s) {
        return new GL3DVec2f(vec1.x * s, vec1.y * s);
    }

    public float dot(GL3DVec2f vec) {
        return GL3DVec2f.dot(this, vec);
    }

    public static float dot(GL3DVec2f u, GL3DVec2f v) {
        return (u.x * v.x) + (u.y * v.y);
    }

    public void negate() {
        this.x = -this.x;
        this.y = -this.y;
    }

    public static GL3DVec2f negate(GL3DVec2f vec) {
        GL3DVec2f newVec = vec.copy();
        newVec.negate();
        return newVec;
    }

    private GL3DVec2f copy() {
        return new GL3DVec2f(this.x, this.y);
    }

    public boolean isApproxEqual(GL3DVec2f vec, float tolerance) {
        return Math.abs(this.x - vec.x) <= tolerance && Math.abs(this.y - vec.y) <= tolerance;
    }

    public float length() {
        return (float) Math.sqrt(this.x * this.x + this.y * this.y);
    }

    public float length2() {
        return (this.x * this.x + this.y * this.y);
    }

    public void normalize() {
        this.divide(length());
    }

    public float[] toArray() {
        return new float[] { x, y };
    }

    public boolean equals(Object o) {
        if (o instanceof GL3DVec2f)
            return isApproxEqual((GL3DVec2f) o, 0.0f);
        return false;
    }

    public Object clone() {
        return new GL3DVec2f(this);
    }

    public static float[] toArray(GL3DVec2f[] vecs) {
        float[] arr = new float[vecs.length * 3];
        for (int i = 0; i < vecs.length; i++) {
            GL3DVec2f v = vecs[i];
            arr[i * 2 + 0] = v.x;
            arr[i * 2 + 1] = v.y;
        }
        return arr;
    }

}
