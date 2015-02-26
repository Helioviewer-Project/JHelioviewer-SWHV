package org.helioviewer.gl3d.scenegraph.math;

public class GL3DVec3d {
    /**
     * Predefined Vectors
     */
    public static final GL3DVec3d ZERO = new GL3DVec3d(0, 0, 0);
    public static final GL3DVec3d XAxis = new GL3DVec3d(1, 0, 0);
    public static final GL3DVec3d YAxis = new GL3DVec3d(0, 1, 0);
    public static final GL3DVec3d ZAxis = new GL3DVec3d(0, 0, 1);

    /**
     * Coordinates
     */
    public double x;
    public double y;
    public double z;

    // Constructors

    public GL3DVec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public GL3DVec3d(GL3DVec2d vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = 0;
    }

    public GL3DVec3d(GL3DVec3d vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = vector.z;
    }

    public GL3DVec3d() {
        this(GL3DVec3d.ZERO);
    }

    public GL3DVec3d(double[] coordinates) {
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

    public final void set(GL3DVec3d vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = vector.z;
    }

    public final void setMax(GL3DVec3d vector) {
        this.x = this.x > vector.x ? this.x : vector.x;
        this.y = this.y > vector.y ? this.y : vector.y;
        this.z = this.z > vector.z ? this.z : vector.z;
    }

    public final void setMin(GL3DVec3d vector) {
        this.x = this.x < vector.x ? this.x : vector.x;
        this.y = this.y < vector.y ? this.y : vector.y;
        this.z = this.z < vector.z ? this.z : vector.z;
    }

    public final void add(GL3DVec3d vec) {
        this.x += vec.x;
        this.y += vec.y;
        this.z += vec.z;
    }

    public final void add(double s) {
        this.x += s;
        this.y += s;
        this.z += s;
    }

    public final static GL3DVec3d add(GL3DVec3d vec1, GL3DVec3d vec2) {
        return new GL3DVec3d(vec1.x + vec2.x, vec1.y + vec2.y, vec1.z + vec2.z);
    }

    public final static GL3DVec3d add(GL3DVec3d vec1, double s) {
        return new GL3DVec3d(vec1.x + s, vec1.y + s, vec1.z + s);
    }

    public final GL3DVec3d subtract(GL3DVec3d vec) {
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

    public final static GL3DVec3d subtract(GL3DVec3d vec1, GL3DVec3d vec2) {
        return new GL3DVec3d(vec1.x - vec2.x, vec1.y - vec2.y, vec1.z - vec2.z);
    }

    public final static GL3DVec3d subtract(GL3DVec3d vec1, double s) {
        return new GL3DVec3d(vec1.x - s, vec1.y - s, vec1.z - s);
    }

    public final void divide(GL3DVec3d vec) {
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

    public final static GL3DVec3d divide(GL3DVec3d vec1, GL3DVec3d vec2) {
        if (vec2.x == 0.0 || vec2.y == 0.0 || vec2.z == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new GL3DVec3d(vec1.x / vec2.x, vec1.y / vec2.y, vec1.z / vec2.z);
    }

    public final static GL3DVec3d divide(GL3DVec3d vec1, double s) {
        if (s == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new GL3DVec3d(vec1.x / s, vec1.y / s, vec1.z / s);
    }

    public final void multiply(GL3DVec3d vec) {
        this.x *= vec.x;
        this.y *= vec.y;
        this.z *= vec.z;
    }

    public final void multiply(double s) {
        this.x *= s;
        this.y *= s;
        this.z *= s;
    }

    public final static GL3DVec3d multiply(GL3DVec3d vec1, GL3DVec3d vec2) {
        return new GL3DVec3d(vec1.x * vec2.x, vec1.y * vec2.y, vec1.z * vec2.z);
    }

    public final static GL3DVec3d multiply(GL3DVec3d vec1, double s) {
        return new GL3DVec3d(vec1.x * s, vec1.y * s, vec1.z * s);
    }

    public final double dot(GL3DVec3d vec) {
        return GL3DVec3d.dot(this, vec);
    }

    public final static double dot(GL3DVec3d u, GL3DVec3d v) {
        return (u.x * v.x) + (u.y * v.y) + (u.z * v.z);
    }

    public final GL3DVec3d cross(GL3DVec3d vec) {
        return GL3DVec3d.cross(this, vec);
    }

    public final static GL3DVec3d cross(GL3DVec3d u, GL3DVec3d v) {
        return new GL3DVec3d(u.y * v.z - u.z * v.y, u.z * v.x - u.x * v.z, u.x * v.y - u.y * v.x);
    }

    public final void negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
    }

    public final static GL3DVec3d negate(GL3DVec3d vec) {
        GL3DVec3d vecCopy = vec.copy();
        vecCopy.negate();
        return vecCopy;
    }

    public final boolean isApproxEqual(GL3DVec3d vec, double tolerance) {
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
        double len = length();
        if (len == 0.0)
            return;

        this.divide(len);

        // take shortcut, reasonably close to 1
        len = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        if (len <= 1.0)
            return;

        // errors up to 2ulp found in testing
        this.divide(Math.nextAfter(len, len + 1.0));

        // take shortcut, reasonably close to 1
        len = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        if (len <= 1.0)
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

    public final GL3DVec2d toVec2() {
        return new GL3DVec2d(x, y);
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof GL3DVec3d)
            return isApproxEqual((GL3DVec3d) o, 0.0);
        return false;
    }

    public final GL3DVec3d copy() {
        return new GL3DVec3d(this);
    }

    public final static double[] toArray(GL3DVec3d[] vecs) {
        double[] arr = new double[vecs.length * 3];
        for (int i = 0; i < vecs.length; i++) {
            GL3DVec3d v = vecs[i];
            arr[i * 3 + 0] = v.x;
            arr[i * 3 + 1] = v.y;
            arr[i * 3 + 2] = v.z;
        }
        return arr;
    }

    @Override
    public final String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
