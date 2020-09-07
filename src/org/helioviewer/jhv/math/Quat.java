package org.helioviewer.jhv.math;

import org.json.JSONArray;

public class Quat {

    public static final Quat ZERO = new Quat();

    final double a;
    final Vec3 u;

    public static Quat createRotation(double angle, Vec3 v) {
        if (angle == 0.)
            return new Quat();

        double halfAngle = angle / 2.;
        double l = v.x * v.x + v.y * v.y + v.z * v.z;
        double m = 0;
        if (l > 0) {
            m = Math.sin(halfAngle) / Math.sqrt(l);
        }

        return new Quat(Math.cos(halfAngle), v.x * m, v.y * m, v.z * m);
    }

    public Quat(double ax, double ay, double az) {
        ax /= 2.;
        ay /= 2.;
        az /= 2.;
        double sx = Math.sin(ax), cx = Math.cos(ax);
        double sy = Math.sin(ay), cy = Math.cos(ay);
        double sz = Math.sin(az), cz = Math.cos(az);

        a = cx * cy * cz + sx * sy * sz;
        u = new Vec3(sx * cy * cz - cx * sy * sz, cx * sy * cz + sx * cy * sz, sx * sy * cz - cx * cy * sz);
    }

    public Quat(double ax, double ay) {
        ax /= 2.;
        ay /= 2.;
        double sx = Math.sin(ax), cx = Math.cos(ax);
        double sy = Math.sin(ay), cy = Math.cos(ay);

        a = cx * cy;
        u = new Vec3(sx * cy, cx * sy, sx * sy);
    }

    private Quat(double _a, double x, double y, double z) {
        this(_a, new Vec3(x, y, z));
    }

    private Quat(double _a, Vec3 _u) {
        a = _a;
        u = _u;
    }

    private Quat() {
        this(1, new Vec3());
    }

    /*
        public Mat4 toMatrix() {
            double w = a, w2 = w * w;
            double x = u.x, x2 = x * x;
            double y = u.y, y2 = y * y;
            double z = u.z, z2 = z * z;

            return new Mat4(w2 + x2 - y2 - z2,     2 * x * y - 2 * w * z, 2 * x * z + 2 * w * y, 0,
                            2 * x * y + 2 * w * z, w2 - x2 + y2 - z2,     2 * y * z - 2 * w * x, 0,
                            2 * x * z - 2 * w * y, 2 * y * z + 2 * w * x, w2 - x2 - y2 + z2,     0,
                            0,                     0,                     0,                     w2 + x2 + y2 + z2);
        }

        public Mat4 toMatrixTranspose() {
            double w = a, w2 = w * w;
            double x = u.x, x2 = x * x;
            double y = u.y, y2 = y * y;
            double z = u.z, z2 = z * z;

            return new Mat4(w2 + x2 - y2 - z2,     2 * x * y + 2 * w * z, 2 * x * z - 2 * w * y, 0,
                            2 * x * y - 2 * w * z, w2 - x2 + y2 - z2,     2 * y * z + 2 * w * x, 0,
                            2 * x * z + 2 * w * y, 2 * y * z - 2 * w * x, w2 - x2 - y2 + z2,     0,
                            0,                     0,                     0,                     w2 + x2 + y2 + z2);
        }

        public double getAngle() {
            return a;
        }

        public void clear() {
            this.a = 1;
            this.u = new Vec3();
        }

        public Quat multiply(Quat q) {
            double a1 = this.a;
            double x1 = this.u.x;
            double y1 = this.u.y;
            double z1 = this.u.z;
            double a2 = q.a;
            double x2 = q.u.x;
            double y2 = q.u.y;
            double z2 = q.u.z;

            double an = a1 * a2 - x1 * x2 - y1 * y2 - z1 * z2;
            double x = a1 * x2 + x1 * a2 + y1 * z2 - z1 * y2;
            double y = a1 * y2 - x1 * z2 + y1 * a2 + z1 * x2;
            double z = a1 * z2 + x1 * y2 - y1 * x2 + z1 * a2;
            return new Quat(an, x, y, z);
        }

        public Quat add(Quat q) {
            this.u.add(q.u);
            this.a += q.a;
            return this;
        }

        public Quat subtract(Quat q) {
            this.u.subtract(q.u);
            this.a -= q.a;
            return this;
        }

        public Quat scale(double s) {
            this.a *= s;
            this.u.multiply(s);
            return this;
        }
    */
    public static Quat rotate(Quat q1, Quat q2) {
        return new Quat(
                q1.a * q2.a - q1.u.x * q2.u.x - q1.u.y * q2.u.y - q1.u.z * q2.u.z,
                q1.a * q2.u.x + q1.u.x * q2.a + q1.u.y * q2.u.z - q1.u.z * q2.u.y,
                q1.a * q2.u.y + q1.u.y * q2.a + q1.u.z * q2.u.x - q1.u.x * q2.u.z,
                q1.a * q2.u.z + q1.u.z * q2.a + q1.u.x * q2.u.y - q1.u.y * q2.u.x);
    }

    public static Quat rotateWithConjugate(Quat q1, Quat q2) {
        return new Quat(
                q1.a * q2.a + q1.u.x * q2.u.x + q1.u.y * q2.u.y + q1.u.z * q2.u.z,
                -q1.a * q2.u.x + q1.u.x * q2.a - q1.u.y * q2.u.z + q1.u.z * q2.u.y,
                -q1.a * q2.u.y + q1.u.y * q2.a - q1.u.z * q2.u.x + q1.u.x * q2.u.z,
                -q1.a * q2.u.z + q1.u.z * q2.a - q1.u.x * q2.u.y + q1.u.y * q2.u.x);
    }
/*
    public Quat slerp(Quat r, double t) {
        double cosAngle = dot(r);

        if (cosAngle > 1 - EPSILON) {
            Quat result = r.copy().add(this.copy().subtract(r).scale(t));
            result.normalize();
            return result;
        }

        if (cosAngle < 0)
            cosAngle = 0;
        if (cosAngle > 1)
            cosAngle = 1;

        Quat v2 = r.copy().subtract(this.copy().scale(cosAngle));
        v2.normalize();

        double theta0 = Math.acos(cosAngle);
        double theta = theta0 * t;
        Quat q = this.copy().scale(Math.cos(theta)).add(v2.scale(Math.sin(theta)));
        q.normalize();
        return q;
    }

    public Quat nlerp(Quat r, double t) {
        Quat result = r.copy().add(this.copy().subtract(r).scale(t));
        result.normalize();
        return result;
    }

    public void set(Quat q) {
        this.a = q.a;
        this.u = q.u;
    }
    public double dot(Quat q) {
        return this.a * q.a + this.u.x * q.u.x + this.u.y * q.u.y + this.u.z * q.u.z;
    }

    public void conjugate() {
        u.x = -u.x;
        u.y = -u.y;
        u.z = -u.z;
    }

    public Quat copy() {
        return new Quat(this.a, this.u.copy());
    }
*/

    private Quat normalize() {
        double l = Math.sqrt(a * a + u.x * u.x + u.y * u.y + u.z * u.z);
        return l == 0 ? this : new Quat(a / l, u.x / l, u.y / l, u.z / l);
    }

    public Quat twist(Vec3 vec) {
        double m = Vec3.dot(vec, u); // / vec.length(); assume vec normalized
        return new Quat(a, new Vec3(vec.x * m, vec.y * m, vec.z * m)).normalize();
        // swing = rotateWithConjugate(q, twist(q))
    }

    public static Quat calcRotation(Vec3 startPoint, Vec3 endPoint) {
        Vec3 rotationAxis = Vec3.cross(startPoint, endPoint);
        double rotationAngle = Math.atan2(rotationAxis.length(), Vec3.dot(startPoint, endPoint));
        return createRotation(rotationAngle, rotationAxis);
    }

    public Vec3 rotateVector(Vec3 vec) {
        //q'vq = vec + 2.0 * cross(q.xyz,cross(  q.xyz, vec ) + q.w * vec)
        double vx = vec.z * u.y - vec.y * u.z + a * vec.x;
        double vy = vec.x * u.z - vec.z * u.x + a * vec.y;
        double vz = vec.y * u.x - vec.x * u.y + a * vec.z;
        double vvx = (vz * u.y - vy * u.z) * 2. + vec.x;
        double vvy = (vx * u.z - vz * u.x) * 2. + vec.y;
        double vvz = (vy * u.x - vx * u.y) * 2. + vec.z;
        return new Vec3(vvx, vvy, vvz);
        //18 mul + 12 add
    }

    public Vec3 rotateInverseVector(Vec3 vec) {
        double vx = -vec.z * u.y + vec.y * u.z + a * vec.x;
        double vy = -vec.x * u.z + vec.z * u.x + a * vec.y;
        double vz = -vec.y * u.x + vec.x * u.y + a * vec.z;
        double vvx = (-vz * u.y + vy * u.z) * 2. + vec.x;
        double vvy = (-vx * u.z + vz * u.x) * 2. + vec.y;
        double vvz = (-vy * u.x + vx * u.y) * 2. + vec.z;
        return new Vec3(vvx, vvy, vvz);
    }

    public void setFloatArray(float[] arr, int idx) {
        arr[idx + 0] = (float) u.x;
        arr[idx + 1] = (float) u.y;
        arr[idx + 2] = (float) u.z;
        arr[idx + 3] = (float) a;
    }

    public JSONArray toJson() {
        return new JSONArray(new double[]{a, u.x, u.y, u.z});
    }

    public static Quat fromJson(JSONArray ja) {
        try {
            return new Quat(ja.getDouble(0), ja.getDouble(1), ja.getDouble(2), ja.getDouble(3));
        } catch (Exception e) {
            return ZERO;
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Quat))
            return false;
        Quat q = (Quat) o;
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(q.a) && u.equals(q.u);
    }

    @Override
    public int hashCode() {
        int result = 1;
        long tmp = Double.doubleToLongBits(a);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        return 31 * result + u.hashCode();
    }

    @Override
    public String toString() {
        return toJson().toString(0); // JSONException
    }

}
