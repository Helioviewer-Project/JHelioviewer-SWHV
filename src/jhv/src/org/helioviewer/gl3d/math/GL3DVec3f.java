package org.helioviewer.gl3d.math;

public class GL3DVec3f {
    /**
     * Predefined Vectors
     */
    public static final GL3DVec3f ZERO = new GL3DVec3f(0f, 0f, 0f);
    public static final GL3DVec3f XAxis = new GL3DVec3f(1f, 0f, 0f);
    public static final GL3DVec3f YAxis = new GL3DVec3f(0f, 1f, 0f);
    public static final GL3DVec3f ZAxis = new GL3DVec3f(0f, 0f, 1f);

    /**
     * Coordinates
     */
    public float x;
    public float y;
    public float z;

    // Constructors

    public GL3DVec3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public GL3DVec3f(GL3DVec2f vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = 0;
    }

    public GL3DVec3f(GL3DVec3f vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = vector.z;
    }

    public GL3DVec3f() {
        this(GL3DVec3f.ZERO);
    }

    public GL3DVec3f(float[] coordinates) {
        if (coordinates == null || coordinates.length < 3) {
            throw new IllegalArgumentException("Coordinate Array must contain at least 3 dimensions");
        }
        this.x = coordinates[0];
        this.y = coordinates[1];
        this.z = coordinates[2];
    }

    public void add(GL3DVec3f vec) {
        this.x += vec.x;
        this.y += vec.y;
        this.z += vec.z;
    }

    public void add(float s) {
        this.x += s;
        this.y += s;
        this.z += s;
    }

    public static GL3DVec3f add(GL3DVec3f vec1, GL3DVec3f vec2) {
        return new GL3DVec3f(vec1.x + vec2.x, vec1.y + vec2.y, vec1.z + vec2.z);
    }

    public static GL3DVec3f add(GL3DVec3f vec1, float s) {
        return new GL3DVec3f(vec1.x + s, vec1.y + s, vec1.z + s);
    }

    public void subtract(GL3DVec3f vec) {
        this.x -= vec.x;
        this.y -= vec.y;
        this.z -= vec.z;
    }

    public void subtract(float s) {
        this.x -= s;
        this.y -= s;
        this.z -= s;
    }

    public static GL3DVec3f subtract(GL3DVec3f vec1, GL3DVec3f vec2) {
        return new GL3DVec3f(vec1.x - vec2.x, vec1.y - vec2.y, vec1.z - vec2.z);
    }

    public static GL3DVec3f subtract(GL3DVec3f vec1, float s) {
        return new GL3DVec3f(vec1.x - s, vec1.y - s, vec1.z - s);
    }

    public void divide(GL3DVec3f vec) {
        if (vec.x == 0f || vec.y == 0f || vec.z == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= vec.x;
        this.y /= vec.y;
        this.z /= vec.z;
    }

    public void divide(float s) {
        if (s == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= s;
        this.y /= s;
        this.z /= s;
    }

    public static GL3DVec3f divide(GL3DVec3f vec1, GL3DVec3f vec2) {
        if (vec2.x == 0f || vec2.y == 0f || vec2.z == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new GL3DVec3f(vec1.x / vec2.x, vec1.y / vec2.y, vec1.z / vec2.z);
    }

    public static GL3DVec3f divide(GL3DVec3f vec1, float s) {
        if (s == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new GL3DVec3f(vec1.x / s, vec1.y / s, vec1.z / s);
    }

    public void multiply(GL3DVec3f vec) {
        this.x *= vec.x;
        this.y *= vec.y;
        this.z *= vec.z;
    }

    public void multiply(float s) {
        this.x *= s;
        this.y *= s;
        this.z *= s;
    }

    public static GL3DVec3f multiply(GL3DVec3f vec1, GL3DVec3f vec2) {
        return new GL3DVec3f(vec1.x * vec2.x, vec1.y * vec2.y, vec1.z * vec2.z);
    }

    public static GL3DVec3f multiply(GL3DVec3f vec1, float s) {
        return new GL3DVec3f(vec1.x * s, vec1.y * s, vec1.z * s);
    }

    public float dot(GL3DVec3f vec) {
        return GL3DVec3f.dot(this, vec);
    }

    public static float dot(GL3DVec3f u, GL3DVec3f v) {
        return (u.x * v.x) + (u.y * v.y) + (u.z * v.z);
    }

    public GL3DVec3f cross(GL3DVec3f vec) {
        return GL3DVec3f.cross(this, vec);
    }

    public static GL3DVec3f cross(GL3DVec3f u, GL3DVec3f v) {
        return new GL3DVec3f(u.y * v.z - u.z * v.y, u.z * v.x - u.x * v.z, u.x * v.y - u.y * v.x);
    }

    public void negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
    }

    public static GL3DVec3f negate(GL3DVec3f vec) {
        GL3DVec3f newVec = vec.copy();
        newVec.negate();
        return newVec;
    }

    private GL3DVec3f copy() {
        return new GL3DVec3f(this.x, this.y, this.z);
    }

    public boolean isApproxEqual(GL3DVec3f vec, float tolerance) {
        return Math.abs(this.x - vec.x) <= tolerance && Math.abs(this.y - vec.y) <= tolerance && Math.abs(this.z - vec.z) <= tolerance;
    }

    public float length() {
        return (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public float length2() {
        return (this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public void normalize() {
        this.divide(length());
    }

    public float[] toArray() {
        return new float[] { x, y, z };
    }

    public GL3DVec2d toVec2() {
        return new GL3DVec2d(x, y);
    }

    public boolean equals(Object o) {
        if (o instanceof GL3DVec3f)
            return isApproxEqual((GL3DVec3f) o, 0.0f);
        return false;
    }

    public Object clone() {
        return new GL3DVec3f(this);
    }

    public static float[] toArray(GL3DVec3f[] vecs) {
        float[] arr = new float[vecs.length * 3];
        for (int i = 0; i < vecs.length; i++) {
            GL3DVec3f v = vecs[i];
            arr[i * 3 + 0] = v.x;
            arr[i * 3 + 1] = v.y;
            arr[i * 3 + 2] = v.z;
        }
        return arr;
    }

}
