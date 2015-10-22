package org.helioviewer.jhv.base.math;

public class Vec4d {
    /**
     * Predefined Vectors
     */

    /**
     * Coordinates
     */
    public double x;
    public double y;
    public double z;
    public double w;

    // Constructors

    public Vec4d(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vec4d(Vec4d vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = vector.z;
        this.w = vector.w;
    }

    public Vec4d(Vec3d vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = vector.z;
        this.w = 0.0;
    }

    public Vec4d() {
        this(0, 0, 0, 0);
    }

    public Vec4d(double[] coordinates) {
        if (coordinates == null || coordinates.length < 4) {
            throw new IllegalArgumentException("Coordinate Array must contain at least 4 dimensions");
        }
        this.x = coordinates[0];
        this.y = coordinates[1];
        this.z = coordinates[2];
        this.w = coordinates[3];
    }

    public void add(Vec4d vec) {
        this.x += vec.x;
        this.y += vec.y;
        this.z += vec.z;
        this.w += vec.w;
    }

    public void add(double s) {
        this.x += s;
        this.y += s;
        this.z += s;
        this.w += w;
    }

    public static Vec4d add(Vec4d vec1, Vec4d vec2) {
        return new Vec4d(vec1.x + vec2.x, vec1.y + vec2.y, vec1.z + vec2.z, vec1.w + vec2.w);
    }

    public static Vec4d add(Vec4d vec1, double s) {
        return new Vec4d(vec1.x + s, vec1.y + s, vec1.z + s, vec1.w + s);
    }

    public void subtract(Vec4d vec) {
        this.x -= vec.x;
        this.y -= vec.y;
        this.z -= vec.z;
        this.w -= vec.w;
    }

    public void subtract(double s) {
        this.x -= s;
        this.y -= s;
        this.z -= s;
        this.w -= s;
    }

    public static Vec4d subtract(Vec4d vec1, Vec4d vec2) {
        return new Vec4d(vec1.x - vec2.x, vec1.y - vec2.y, vec1.z - vec2.z, vec1.w - vec2.w);
    }

    public static Vec4d subtract(Vec4d vec1, double s) {
        return new Vec4d(vec1.x - s, vec1.y - s, vec1.z - s, vec1.w - s);
    }

    public void divide(Vec4d vec) {
        if (vec.x == 0 || vec.y == 0 || vec.z == 0 || vec.w == 0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= vec.x;
        this.y /= vec.y;
        this.z /= vec.z;
        this.w /= vec.w;
    }

    public void divide(double s) {
        if (s == 0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= s;
        this.y /= s;
        this.z /= s;
        this.w /= s;
    }

    public static Vec4d divide(Vec4d vec1, Vec4d vec2) {
        if (vec2.x == 0 || vec2.y == 0 || vec2.z == 0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new Vec4d(vec1.x / vec2.x, vec1.y / vec2.y, vec1.z / vec2.z, vec1.w / vec2.w);
    }

    public static Vec4d divide(Vec4d vec1, double s) {
        if (s == 0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new Vec4d(vec1.x / s, vec1.y / s, vec1.z / s, vec1.w / s);
    }

    public void multiply(Vec4d vec) {
        this.x *= vec.x;
        this.y *= vec.y;
        this.z *= vec.z;
        this.w *= vec.w;
    }

    public void multiply(double s) {
        this.x *= s;
        this.y *= s;
        this.z *= s;
        this.w *= s;
    }

    public static Vec4d multiply(Vec4d vec1, Vec4d vec2) {
        return new Vec4d(vec1.x * vec2.x, vec1.y * vec2.y, vec1.z * vec2.z, vec1.w * vec2.w);
    }

    public static Vec4d multiply(Vec4d vec1, double s) {
        return new Vec4d(vec1.x * s, vec1.y * s, vec1.z * s, vec1.w * s);
    }

    public double dot(Vec4d vec) {
        return Vec4d.dot(this, vec);
    }

    public static double dot(Vec4d u, Vec4d v) {
        return (u.x * v.x) + (u.y * v.y) + (u.z * v.z) + (u.w * v.w);
    }

    public static double dot3d(Vec4d u, Vec4d v) {
        return (u.x * v.x) + (u.y * v.y) + (u.z * v.z);
    }

    public Vec4d cross(Vec4d vec) {
        return Vec4d.cross(this, vec);
    }

    public static Vec4d cross(Vec4d u, Vec4d v) {
        return new Vec4d(u.y * v.z - u.z * v.y, u.z * v.x - u.x * v.z, u.x * v.y - u.y * v.x, 1);
    }

    public void negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        this.w = -this.w;
    }

    public static Vec4d negate(Vec4d vec) {
        Vec4d newVec = vec.copy();
        newVec.negate();
        return newVec;
    }

    private Vec4d copy() {
        return new Vec4d(this.x, this.y, this.z, this.w);
    }

    public boolean isApproxEqual(Vec4d vec, double tolerance) {
        return Math.abs(this.x - vec.x) <= tolerance && Math.abs(this.y - vec.y) <= tolerance && Math.abs(this.z - vec.z) <= tolerance && Math.abs(this.w - vec.w) <= tolerance;
    }

    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);
    }

    public double length2() {
        return this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w;
    }

    public final void normalize() {
        double len, len2;

        len2 = this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w;
        if (len2 == 0.0)
            return;

        len = Math.sqrt(len2);
        this.x /= len;
        this.y /= len;
        this.z /= len;
        this.w /= len;

        // take shortcut, reasonably close to 1
        len2 = this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w;
        if (len2 <= 1.0)
            return;

        // errors up to 2ulp found in testing
        double next = Math.sqrt(Math.nextAfter(len2, len2 + 1.0));
        this.x /= next;
        this.y /= next;
        this.z /= next;
        this.w /= next;

        // take shortcut, reasonably close to 1
        len2 = this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w;
        if (len2 <= 1.0)
            return;

        // can't happen / something is really messed up
        /*
         * System.out.println(len); System.out.println(this);
         * Log.error("The length of the vector is bigger than 1");
         */
        // System.exit(1);

        this.x = Double.NaN;
        this.y = Double.NaN;
        this.z = Double.NaN;
        this.w = Double.NaN;
    }

    public double[] toArray() {
        return new double[] { x, y, z, w };
    }

    public static double[] toArray(Vec4d[] vecs) {
        double[] arr = new double[vecs.length * 4];
        for (int i = 0; i < vecs.length; i++) {
            Vec4d v = vecs[i];
            arr[i * 4 + 0] = v.x;
            arr[i * 4 + 1] = v.y;
            arr[i * 4 + 2] = v.z;
            arr[i * 4 + 3] = v.w;
        }
        return arr;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Vec4d)
            return isApproxEqual((Vec4d) o, 0);
        return false;
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + ", " + z + ", " + w + "]";
    }

}
