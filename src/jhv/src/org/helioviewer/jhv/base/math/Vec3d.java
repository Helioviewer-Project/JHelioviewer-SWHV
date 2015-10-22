package org.helioviewer.jhv.base.math;

public class Vec3d {
    /**
     * Predefined Vectors
     */
    public static final Vec3d ZERO = new Vec3d(0, 0, 0);
    public static final Vec3d XAxis = new Vec3d(1, 0, 0);
    public static final Vec3d YAxis = new Vec3d(0, 1, 0);
    public static final Vec3d ZAxis = new Vec3d(0, 0, 1);

    /**
     * Coordinates
     */
    public double x;
    public double y;
    public double z;

    // Constructors

    public Vec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3d(Vec2d vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = 0;
    }

    public Vec3d(Vec3d vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = vector.z;
    }

    public Vec3d() {
        this(Vec3d.ZERO);
    }

    public Vec3d(double[] coordinates) {
        if (coordinates == null || coordinates.length < 3) {
            throw new IllegalArgumentException("Coordinate Array must contain at least 3 dimensions");
        }
        this.x = coordinates[0];
        this.y = coordinates[1];
        this.z = coordinates[2];
    }

    public final void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public final void set(Vec3d vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = vector.z;
    }

    public final void setMax(Vec3d vector) {
        this.x = this.x > vector.x ? this.x : vector.x;
        this.y = this.y > vector.y ? this.y : vector.y;
        this.z = this.z > vector.z ? this.z : vector.z;
    }

    public final void setMin(Vec3d vector) {
        this.x = this.x < vector.x ? this.x : vector.x;
        this.y = this.y < vector.y ? this.y : vector.y;
        this.z = this.z < vector.z ? this.z : vector.z;
    }

    public final void add(Vec3d vec) {
        this.x += vec.x;
        this.y += vec.y;
        this.z += vec.z;
    }

    public final void add(double s) {
        this.x += s;
        this.y += s;
        this.z += s;
    }

    public final static Vec3d add(Vec3d vec1, Vec3d vec2) {
        return new Vec3d(vec1.x + vec2.x, vec1.y + vec2.y, vec1.z + vec2.z);
    }

    public final static Vec3d add(Vec3d vec1, double s) {
        return new Vec3d(vec1.x + s, vec1.y + s, vec1.z + s);
    }

    public final Vec3d subtract(Vec3d vec) {
        this.x -= vec.x;
        this.y -= vec.y;
        this.z -= vec.z;
        return this;
    }

    public final void subtract(double s) {
        this.x -= s;
        this.y -= s;
        this.z -= s;
    }

    public final static Vec3d subtract(Vec3d vec1, Vec3d vec2) {
        return new Vec3d(vec1.x - vec2.x, vec1.y - vec2.y, vec1.z - vec2.z);
    }

    public final static Vec3d subtract(Vec3d vec1, double s) {
        return new Vec3d(vec1.x - s, vec1.y - s, vec1.z - s);
    }

    public final void divide(Vec3d vec) {
        if (vec.x == 0.0 || vec.y == 0.0 || vec.z == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= vec.x;
        this.y /= vec.y;
        this.z /= vec.z;
    }

    public final void divide(double s) {
        if (s == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= s;
        this.y /= s;
        this.z /= s;
    }

    public final static Vec3d divide(Vec3d vec1, Vec3d vec2) {
        if (vec2.x == 0.0 || vec2.y == 0.0 || vec2.z == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new Vec3d(vec1.x / vec2.x, vec1.y / vec2.y, vec1.z / vec2.z);
    }

    public final static Vec3d divide(Vec3d vec1, double s) {
        if (s == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new Vec3d(vec1.x / s, vec1.y / s, vec1.z / s);
    }

    public final void multiply(Vec3d vec) {
        this.x *= vec.x;
        this.y *= vec.y;
        this.z *= vec.z;
    }

    public final void multiply(double s) {
        this.x *= s;
        this.y *= s;
        this.z *= s;
    }

    public final static Vec3d multiply(Vec3d vec1, Vec3d vec2) {
        return new Vec3d(vec1.x * vec2.x, vec1.y * vec2.y, vec1.z * vec2.z);
    }

    public final static Vec3d multiply(Vec3d vec1, double s) {
        return new Vec3d(vec1.x * s, vec1.y * s, vec1.z * s);
    }

    public final double dot(Vec3d vec) {
        return Vec3d.dot(this, vec);
    }

    public final static double dot(Vec3d u, Vec3d v) {
        return (u.x * v.x) + (u.y * v.y) + (u.z * v.z);
    }

    public final Vec3d cross(Vec3d vec) {
        return Vec3d.cross(this, vec);
    }

    public final static Vec3d cross(Vec3d u, Vec3d v) {
        return new Vec3d(u.y * v.z - u.z * v.y, u.z * v.x - u.x * v.z, u.x * v.y - u.y * v.x);
    }

    public final void negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
    }

    public final static Vec3d negate(Vec3d vec) {
        Vec3d vecCopy = vec.copy();
        vecCopy.negate();
        return vecCopy;
    }

    public final boolean isApproxEqual(Vec3d vec, double tolerance) {
        return Math.abs(this.x - vec.x) <= tolerance && Math.abs(this.y - vec.y) <= tolerance && Math.abs(this.z - vec.z) <= tolerance;
    }

    public final double length() {
        double absmax = Math.max(Math.max(Math.abs(this.x), Math.abs(this.y)), Math.abs(this.z));
        if (absmax == 0.0)
            return 0.0;

        double tmpx = this.x / absmax;
        double tmpy = this.y / absmax;
        double tmpz = this.z / absmax;
        return absmax * Math.sqrt(tmpx * tmpx + tmpy * tmpy + tmpz * tmpz);
    }

    public final double length2() {
        double len = length();
        return len * len;
    }

    public final void normalize() {
        double len, len2;

        len2 = this.x * this.x + this.y * this.y + this.z * this.z;
        if (len2 == 0.0)
            return;

        len = Math.sqrt(len2);
        this.x /= len;
        this.y /= len;
        this.z /= len;

        // take shortcut, reasonably close to 1
        len2 = this.x * this.x + this.y * this.y + this.z * this.z;
        if (len2 <= 1.0)
            return;

        // errors up to 2ulp found in testing
        double next = Math.sqrt(Math.nextAfter(len2, len2 + 1.0));
        this.x /= next;
        this.y /= next;
        this.z /= next;

        // take shortcut, reasonably close to 1
        len2 = this.x * this.x + this.y * this.y + this.z * this.z;
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
    }

    public final double[] toArray() {
        return new double[] { x, y, z };
    }

    public final Vec2d toVec2() {
        return new Vec2d(x, y);
    }

    public final Vec3d copy() {
        return new Vec3d(this);
    }

    public final static double[] toArray(Vec3d[] vecs) {
        double[] arr = new double[vecs.length * 3];
        for (int i = 0; i < vecs.length; i++) {
            Vec3d v = vecs[i];
            arr[i * 3 + 0] = v.x;
            arr[i * 3 + 1] = v.y;
            arr[i * 3 + 2] = v.z;
        }
        return arr;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof Vec3d)
            return isApproxEqual((Vec3d) o, 0.0);
        return false;
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

    @Override
    public final String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

}
