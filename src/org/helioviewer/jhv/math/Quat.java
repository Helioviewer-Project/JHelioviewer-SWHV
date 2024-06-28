package org.helioviewer.jhv.math;

import org.json.JSONArray;

public class Quat {

    public static final Quat ZERO = new Quat(1, 0, 0, 0);
    public static final Quat X90 = Quat.createAxisX(Math.PI / 2);
    public static final Quat Y90 = Quat.createAxisY(Math.PI / 2);
    public static final Quat Z90 = Quat.createAxisZ(Math.PI / 2);

    final double w;
    final double x;
    final double y;
    final double z;

    public static Quat createAxisX(double a) {
        a /= 2.;
        return new Quat(Math.cos(a), Math.sin(a), 0, 0);
    }

    public static Quat createAxisY(double a) {
        a /= 2.;
        double m = Math.sin(a);
        return new Quat(Math.cos(a), 0, Math.sin(a), 0);
    }

    public static Quat createAxisZ(double a) {
        a /= 2.;
        return new Quat(Math.cos(a), 0, 0, Math.sin(a));
    }

    private static Quat createRotation(double a, Vec3 v) {
        if (a == 0.)
            return ZERO;
        a /= 2.;

        double l = v.x * v.x + v.y * v.y + v.z * v.z;
        double m = 0;
        if (l > 0) {
            m = Math.sin(a) / Math.sqrt(l);
        }
        return new Quat(Math.cos(a), m * v.x, m * v.y, m * v.z);
    }
/*
    public static Quat createXYZ(double ax, double ay, double az) {
        ax /= 2.;
        ay /= 2.;
        az /= 2.;
        double sx = Math.sin(ax), cx = Math.cos(ax);
        double sy = Math.sin(ay), cy = Math.cos(ay);
        double sz = Math.sin(az), cz = Math.cos(az);

        return new Quat(
            cx * cy * cz + sx * sy * sz,
            sx * cy * cz - cx * sy * sz,
            cx * sy * cz + sx * cy * sz,
            sx * sy * cz - cx * cy * sz);
    }
*/
    public static Quat createXY(double ax, double ay) {
        ax /= 2.;
        ay /= 2.;
        double sx = Math.sin(ax), cx = Math.cos(ax);
        double sy = Math.sin(ay), cy = Math.cos(ay);

        return new Quat(
            cx * cy,
            sx * cy,
            cx * sy,
            sx * sy);
    }

    public Quat(double _w, double _x, double _y, double _z) {
        w = _w;
        x = _x;
        y = _y;
        z = _z;
    }

    public static Quat rotate(Quat q1, Quat q2) {
        return new Quat(
                q1.w * q2.w - q1.x * q2.x - q1.y * q2.y - q1.z * q2.z,
                q1.w * q2.x + q1.x * q2.w + q1.y * q2.z - q1.z * q2.y,
                q1.w * q2.y + q1.y * q2.w + q1.z * q2.x - q1.x * q2.z,
                q1.w * q2.z + q1.z * q2.w + q1.x * q2.y - q1.y * q2.x);
    }

    public static Quat rotateWithConjugate(Quat q1, Quat q2) {
        return new Quat(
                q1.w * q2.w + q1.x * q2.x + q1.y * q2.y + q1.z * q2.z,
                -q1.w * q2.x + q1.x * q2.w - q1.y * q2.z + q1.z * q2.y,
                -q1.w * q2.y + q1.y * q2.w - q1.z * q2.x + q1.x * q2.z,
                -q1.w * q2.z + q1.z * q2.w - q1.x * q2.y + q1.y * q2.x);
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
*/

    private Quat normalize() {
        double l = Math.sqrt(w * w + x * x + y * y + z * z);
        return l == 0 ? this : new Quat(w / l, x / l, y / l, z / l);
    }

    public Quat twist(Vec3 v) {
        double m = x * v.x + y * v.y + z * v.z; // / v.length(); assume v normalized
        return new Quat(w, m * v.x, m * v.y, m * v.z).normalize();
        // swing = rotateWithConjugate(q, twist(q))
    }

    public static Quat calcRotation(Vec3 startPoint, Vec3 endPoint) {
        Vec3 rotationAxis = Vec3.cross(startPoint, endPoint);
        double rotationAngle = Math.atan2(rotationAxis.length(), Vec3.dot(startPoint, endPoint));
        return createRotation(rotationAngle, rotationAxis);
    }

    public Vec3 rotateVector(Vec3 vec) {
        //q'vq = vec + 2.0 * cross(q.xyz,cross(  q.xyz, vec ) + q.w * vec)
        double vx = vec.z * y - vec.y * z + w * vec.x;
        double vy = vec.x * z - vec.z * x + w * vec.y;
        double vz = vec.y * x - vec.x * y + w * vec.z;
        double vvx = (vz * y - vy * z) * 2. + vec.x;
        double vvy = (vx * z - vz * x) * 2. + vec.y;
        double vvz = (vy * x - vx * y) * 2. + vec.z;
        return new Vec3(vvx, vvy, vvz);
        //18 mul + 12 add
    }

    // rotateVector for array
    public double[] qxv(double[] vec) {
        //q'vq = vec + 2.0 * cross(q.xyz,cross(  q.xyz, vec ) + q.w * vec)
        double vx = vec[2] * y - vec[1] * z + w * vec[0];
        double vy = vec[0] * z - vec[2] * x + w * vec[1];
        double vz = vec[1] * x - vec[0] * y + w * vec[2];

        return new double[]{
                (vz * y - vy * z) * 2. + vec[0],
                (vx * z - vz * x) * 2. + vec[1],
                (vy * x - vx * y) * 2. + vec[2]};
        //18 mul + 12 add
    }

    public Vec3 rotateInverseVector(Vec3 vec) {
        double vx = -vec.z * y + vec.y * z + w * vec.x;
        double vy = -vec.x * z + vec.z * x + w * vec.y;
        double vz = -vec.y * x + vec.x * y + w * vec.z;
        double vvx = (-vz * y + vy * z) * 2. + vec.x;
        double vvy = (-vx * z + vz * x) * 2. + vec.y;
        double vvz = (-vy * x + vx * y) * 2. + vec.z;
        return new Vec3(vvx, vvy, vvz);
    }

    public void setFloatArray(float[] arr, int idx) {
        arr[idx] = (float) x;
        arr[idx + 1] = (float) y;
        arr[idx + 2] = (float) z;
        arr[idx + 3] = (float) w;
    }

    public JSONArray toJson() {
        return new JSONArray(new double[]{w, x, y, z});
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
        if (o instanceof Quat q)
            return Double.doubleToLongBits(w) == Double.doubleToLongBits(q.w) &&
                    Double.doubleToLongBits(x) == Double.doubleToLongBits(q.x) &&
                    Double.doubleToLongBits(y) == Double.doubleToLongBits(q.y) &&
                    Double.doubleToLongBits(z) == Double.doubleToLongBits(q.z);
        return false;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(w);
        result = 31 * result + Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        return 31 * result + Double.hashCode(z);
    }

    @Override
    public String toString() {
        return toJson().toString(0); // JSONException
    }

}
