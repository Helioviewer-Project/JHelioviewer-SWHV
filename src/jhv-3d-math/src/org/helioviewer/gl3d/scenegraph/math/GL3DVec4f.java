package org.helioviewer.gl3d.scenegraph.math;

public class GL3DVec4f {
    /**
     * Predefined Vectors
     */

    /**
     * Coordinates
     */
    public float x;
    public float y;
    public float z;
    public float w;

    // Constructors

    public GL3DVec4f(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public GL3DVec4f(GL3DVec4f vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = vector.z;
        this.w = vector.w;
    }

    public GL3DVec4f() {
        this(0f, 0f, 0f, 0f);
    }

    public GL3DVec4f(float[] coordinates) {
        if (coordinates == null || coordinates.length < 4) {
            throw new IllegalArgumentException("Coordinate Array must contain at least 4 dimensions");
        }
        this.x = coordinates[0];
        this.y = coordinates[1];
        this.z = coordinates[2];
        this.w = coordinates[3];
    }

    public void add(GL3DVec4f vec) {
        this.x += vec.x;
        this.y += vec.y;
        this.z += vec.z;
        this.w += vec.w;
    }

    public void add(float s) {
        this.x += s;
        this.y += s;
        this.z += s;
        this.w += w;
    }

    public static GL3DVec4f add(GL3DVec4f vec1, GL3DVec4f vec2) {
        return new GL3DVec4f(vec1.x + vec2.x, vec1.y + vec2.y, vec1.z + vec2.z, vec1.w + vec2.w);
    }

    public static GL3DVec4f add(GL3DVec4f vec1, float s) {
        return new GL3DVec4f(vec1.x + s, vec1.y + s, vec1.z + s, vec1.w + s);
    }

    public void subtract(GL3DVec4f vec) {
        this.x -= vec.x;
        this.y -= vec.y;
        this.z -= vec.z;
        this.w -= vec.w;
    }

    public void subtract(float s) {
        this.x -= s;
        this.y -= s;
        this.z -= s;
        this.w -= s;
    }

    public static GL3DVec4f subtract(GL3DVec4f vec1, GL3DVec4f vec2) {
        return new GL3DVec4f(vec1.x - vec2.x, vec1.y - vec2.y, vec1.z - vec2.z, vec1.w - vec2.w);
    }

    public static GL3DVec4f subtract(GL3DVec4f vec1, float s) {
        return new GL3DVec4f(vec1.x - s, vec1.y - s, vec1.z - s, vec1.w - s);
    }

    public GL3DVec4f divide(GL3DVec4f vec) {
        if (vec.x == 0f || vec.y == 0f || vec.z == 0f || vec.w == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= vec.x;
        this.y /= vec.y;
        this.z /= vec.z;
        this.w /= vec.w;
        return this;
    }

    public void divide(float s) {
        if (s == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= s;
        this.y /= s;
        this.z /= s;
        this.w /= s;
    }

    public static GL3DVec4f divide(GL3DVec4f vec1, GL3DVec4f vec2) {
        if (vec2.x == 0f || vec2.y == 0f || vec2.z == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new GL3DVec4f(vec1.x / vec2.x, vec1.y / vec2.y, vec1.z / vec2.z, vec1.w / vec2.w);
    }

    public static GL3DVec4f divide(GL3DVec4f vec1, float s) {
        if (s == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new GL3DVec4f(vec1.x / s, vec1.y / s, vec1.z / s, vec1.w / s);
    }

    public void multiply(GL3DVec4f vec) {
        this.x *= vec.x;
        this.y *= vec.y;
        this.z *= vec.z;
        this.w *= vec.w;
    }

    public void multiply(float s) {
        this.x *= s;
        this.y *= s;
        this.z *= s;
        this.w *= s;
    }

    public static GL3DVec4f multiply(GL3DVec4f vec1, GL3DVec4f vec2) {
        return new GL3DVec4f(vec1.x * vec2.x, vec1.y * vec2.y, vec1.z * vec2.z, vec1.w * vec2.w);
    }

    public static GL3DVec4f multiply(GL3DVec4f vec1, float s) {
        return new GL3DVec4f(vec1.x * s, vec1.y * s, vec1.z * s, vec1.w * s);
    }

    public float dot(GL3DVec4f vec) {
        return GL3DVec4f.dot(this, vec);
    }

    public static float dot(GL3DVec4f u, GL3DVec4f v) {
        return (u.x * v.x) + (u.y * v.y) + (u.z * v.z) + (u.w * v.w);
    }

    public GL3DVec4f cross(GL3DVec4f vec) {
        return GL3DVec4f.cross(this, vec);
    }

    public static GL3DVec4f cross(GL3DVec4f u, GL3DVec4f v) {
        return new GL3DVec4f(u.y * v.z - u.z * v.y, u.z * v.x - u.x * v.z, u.x * v.y - u.y * v.x, 1f);
    }

    public void negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        this.w = -this.w;
    }

    public static GL3DVec4f negate(GL3DVec4f vec) {
        GL3DVec4f newVec = vec.copy();
        newVec.negate();
        return newVec;
    }

    private GL3DVec4f copy() {
        return new GL3DVec4f(this.x, this.y, this.z, this.w);
    }

    public boolean isApproxEqual(GL3DVec4f vec, float tolerance) {
        return Math.abs(this.x - vec.x) <= tolerance && Math.abs(this.y - vec.y) <= tolerance && Math.abs(this.z - vec.z) <= tolerance && Math.abs(this.w - vec.w) <= tolerance;
    }

    public float length() {
        return (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);
    }

    public float length2() {
        return (this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);
    }

    public void normalize() {
        this.divide(length());
    }

    public float[] toArray() {
        return new float[] { x, y, z, w };
    }

    public boolean equals(Object o) {
        if (o instanceof GL3DVec4f)
            return isApproxEqual((GL3DVec4f) o, 0.0f);
        return false;
    }

    public static float[] toArray(GL3DVec4f[] vecs) {
        float[] arr = new float[vecs.length * 4];
        for (int i = 0; i < vecs.length; i++) {
            GL3DVec4f v = vecs[i];
            arr[i * 4 + 0] = v.x;
            arr[i * 4 + 1] = v.y;
            arr[i * 4 + 2] = v.z;
            arr[i * 4 + 3] = v.w;
        }
        return arr;
    }
}
