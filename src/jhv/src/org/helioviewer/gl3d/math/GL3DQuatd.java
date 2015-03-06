package org.helioviewer.gl3d.math;

public class GL3DQuatd {
    public static final double EPSILON = 0.000001;

    protected double a;
    protected GL3DVec3d u;

    public static GL3DQuatd createRotation(double angle, GL3DVec3d axis) {
        if (angle == 0.)
            return new GL3DQuatd();

        double halfAngle = angle / 2.0;
        GL3DVec3d axisCopy = axis.copy();
        axisCopy.normalize();
        axisCopy.multiply(Math.sin(halfAngle));
        return new GL3DQuatd(Math.cos(halfAngle), axisCopy);
    }

    private GL3DQuatd(double a, double x, double y, double z) {
        this(a, new GL3DVec3d(x, y, z));
    }

    private GL3DQuatd(double a, GL3DVec3d u) {
        this.a = a;
        this.u = u;
    }

    public GL3DQuatd() {
        this(1, new GL3DVec3d());
    }

    public void clear() {
        // GL3DQuatd q = GL3DQuatd.createRotation(0.0, GL3DVec3d.YAxis);
        this.a = 1;
        this.u = new GL3DVec3d();
    }

    public GL3DQuatd multiply(GL3DQuatd q) {
        double a1 = this.a;
        double x1 = this.u.x;
        double y1 = this.u.y;
        double z1 = this.u.z;
        double a2 = q.a;
        double x2 = q.u.x;
        double y2 = q.u.y;
        double z2 = q.u.z;

        double a = (a1 * a2 - x1 * x2 - y1 * y2 - z1 * z2);
        double x = (a1 * x2 + x1 * a2 + y1 * z2 - z1 * y2);
        double y = (a1 * y2 - x1 * z2 + y1 * a2 + z1 * x2);
        double z = (a1 * z2 + x1 * y2 - y1 * x2 + z1 * a2);
        GL3DQuatd res = new GL3DQuatd(a, x, y, z);

        return res;
    }

    public GL3DMat4d toMatrix() {
        double w = a, w2 = w * w;
        double x = u.x, x2 = x * x;
        double y = u.y, y2 = y * y;
        double z = u.z, z2 = z * z;

        return new GL3DMat4d(w2 + x2 - y2 - z2, 2 * x * y - 2 * w * z, 2 * x * z + 2 * w * y, 0,
        2 * x * y + 2 * w * z, w2 - x2 + y2 - z2, 2 * y * z - 2 * w * x, 0,
        2 * x * z - 2 * w * y, 2 * y * z + 2 * w * x, w2 - x2 - y2 + z2, 0,
        0, 0, 0, w2 + x2 + y2 + z2);
        /*
         * return new GL3DMat4d( w2+x2-y2-z2, 2*x*y+2*w*z, 2*x*z-2*w*y, 0,
         * 
         * 2*x*y-2*w*z, w2-x2+y2-z2, 2*y*z+2*w*x, 0,
         * 
         * 2*x*z+2*w*y, 2*y*z-2*w*x, w2-x2-y2+z2, 0,
         * 
         * 0, 0, 0, 1 );
         */
    }

    public double getAngle() {
        return this.a;
    }

    public GL3DVec3d getRotationAxis() {
        return this.u;
    }

    // public GL3DQuatd interpolate(GL3DQuatd q) {
    // double a = this.a + q.a/2;
    // GL3DVec3d u = this.u.copy().add(q.u).divide(2);
    // return new GL3DQuatd(a, u);
    //

    public GL3DQuatd add(GL3DQuatd q) {
        this.u.add(q.u);
        this.a += q.a;
        return this;
    }

    public GL3DQuatd subtract(GL3DQuatd q) {
        this.u.subtract(q.u);
        this.a -= q.a;
        return this;
    }

    public GL3DQuatd scale(double s) {
        this.a *= s;
        this.u.multiply(s);
        return this;
    }

    public void rotate(GL3DQuatd q2) {
        GL3DQuatd q1 = this.copy();

        this.a = q1.a * q2.a - q1.u.x * q2.u.x - q1.u.y * q2.u.y - q1.u.z * q2.u.z;
        this.u.x = q1.a * q2.u.x + q1.u.x * q2.a + q1.u.y * q2.u.z - q1.u.z * q2.u.y;
        this.u.y = q1.a * q2.u.y + q1.u.y * q2.a + q1.u.z * q2.u.x - q1.u.x * q2.u.z;
        this.u.z = q1.a * q2.u.z + q1.u.z * q2.a + q1.u.x * q2.u.y - q1.u.y * q2.u.x;

        this.normalize();
    }

    public GL3DQuatd slerp(GL3DQuatd r, double t) {
        double cosAngle = dot(r);

        if (cosAngle > 1 - EPSILON) {
            GL3DQuatd result = r.copy().add(this.copy().subtract(r).scale(t));
            result.normalize();
            return result;
        }

        if (cosAngle < 0)
            cosAngle = 0;
        if (cosAngle > 1)
            cosAngle = 1;

        double theta0 = Math.acos(cosAngle);
        double theta = theta0 * t;
        GL3DQuatd v2 = r.copy().subtract(this.copy().scale(cosAngle));
        v2.normalize();

        GL3DQuatd q = this.copy().scale(Math.cos(theta)).add(v2.scale(Math.sin(theta)));
        q.normalize();
        return q;
    }

    public GL3DQuatd nlerp(GL3DQuatd r, double t) {
        GL3DQuatd result = r.copy().add(this.copy().subtract(r).scale(t));
        result.normalize();
        return result;
    }

    public void set(GL3DQuatd q) {
        this.a = q.a;
        this.u = q.u;
    }

    public GL3DQuatd normalize() {
        double l = this.length();
        a /= l;
        u.divide(l);
        return this;
    }

    public double length() {
        return Math.sqrt(length2());
    }

    public double length2() {
        return a * a + u.length2();
    }

    public double dot(GL3DQuatd q) {
        return this.a * q.a + this.u.x * q.u.x + this.u.y * q.u.y + this.u.z * q.u.z;
    }

    public static GL3DQuatd calcRotation(GL3DVec3d startPoint, GL3DVec3d endPoint) {
        GL3DVec3d rotationAxis = GL3DVec3d.cross(startPoint, endPoint);
        double rotationAngle = Math.atan2(rotationAxis.length(), GL3DVec3d.dot(startPoint, endPoint));

        return GL3DQuatd.createRotation(rotationAngle, rotationAxis);
    }

    public GL3DQuatd copy() {
        return new GL3DQuatd(this.a, this.u.copy());
    }

    public String toString() {
        return "[" + a + ", " + u.x + ", " + u.y + ", " + u.z + "]";
    }

}
