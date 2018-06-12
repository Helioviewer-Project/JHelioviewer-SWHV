package org.helioviewer.jhv.math;

public class Mat4 {

    //  0 4 8 12 1 5 9 13 2 6 10 14 3 7 11 15
    public final double[] m = new double[16];

    Mat4(double M0, double M4, double M8, double M12, double M1, double M5, double M9, double M13, double M2, double M6, double M10, double M14, double M3, double M7, double M11, double M15) {
        m[0] = M0;
        m[4] = M4;
        m[8] = M8;
        m[12] = M12;
        m[1] = M1;
        m[5] = M5;
        m[9] = M9;
        m[13] = M13;
        m[2] = M2;
        m[6] = M6;
        m[10] = M10;
        m[14] = M14;
        m[3] = M3;
        m[7] = M7;
        m[11] = M11;
        m[15] = M15;
    }

    private Mat4() {
    }
/*
    public Mat4(Mat4 mat) {
        set(mat);
    }
*/
    public float[] getFloatArray() {
        float[] arr = new float[16];
        for (int i = 0; i < 16; i++) {
            arr[i] = (float) m[i];
        }
        return arr;
    }
/*
    public void setIdentity() {
        set(identity());
    }

    public static Mat4 identity() {
        return new Mat4(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
    }

    public static Mat4 orthoIdentity() {
        return new Mat4(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1);
    }

    public Mat4 set(Mat4 A) {
        m[0] = A.m[0];
        m[4] = A.m[4];
        m[8] = A.m[8];
        m[12] = A.m[12];
        m[1] = A.m[1];
        m[5] = A.m[5];
        m[9] = A.m[9];
        m[13] = A.m[13];
        m[2] = A.m[2];
        m[6] = A.m[6];
        m[10] = A.m[10];
        m[14] = A.m[14];
        m[3] = A.m[3];
        m[7] = A.m[7];
        m[11] = A.m[11];
        m[15] = A.m[15];

        return this;
    }

    public Mat4 set(double M0, double M4, double M8, double M12, double M1, double M5, double M9, double M13, double M2, double M6, double M10, double M14, double M3, double M7, double M11, double M15) {
        m[0] = M0;
        m[4] = M4;
        m[8] = M8;
        m[12] = M12;
        m[1] = M1;
        m[5] = M5;
        m[9] = M9;
        m[13] = M13;
        m[2] = M2;
        m[6] = M6;
        m[10] = M10;
        m[14] = M14;
        m[3] = M3;
        m[7] = M7;
        m[11] = M11;
        m[15] = M15;

        return this;
    }

    public Mat4 set(int index, double f) {
        if (index < 0 || index > 15)
            throw new IndexOutOfBoundsException("Mat4 has 16 fields");

        m[index] = f;
        return this;
    }

    public double get(int index) {
        if (index < 0 || index > 15)
            throw new IndexOutOfBoundsException("Mat4 has 16 fields");

        return m[index];
    }

    public Mat4 multiply(Mat4 A) {
        set(m[0] * A.m[0] + m[4] * A.m[1] + m[8] * A.m[2] + m[12] * A.m[3], // row 1
                m[0] * A.m[4] + m[4] * A.m[5] + m[8] * A.m[6] + m[12] * A.m[7], m[0] * A.m[8] + m[4] * A.m[9] + m[8] * A.m[10] + m[12] * A.m[11], m[0] * A.m[12] + m[4] * A.m[13] + m[8] * A.m[14] + m[12] * A.m[15], m[1] * A.m[0] + m[5] * A.m[1] + m[9] * A.m[2] + m[13] * A.m[3], // row 2
                m[1] * A.m[4] + m[5] * A.m[5] + m[9] * A.m[6] + m[13] * A.m[7], m[1] * A.m[8] + m[5] * A.m[9] + m[9] * A.m[10] + m[13] * A.m[11], m[1] * A.m[12] + m[5] * A.m[13] + m[9] * A.m[14] + m[13] * A.m[15], m[2] * A.m[0] + m[6] * A.m[1] + m[10] * A.m[2] + m[14] * A.m[3], // row 3
                m[2] * A.m[4] + m[6] * A.m[5] + m[10] * A.m[6] + m[14] * A.m[7], m[2] * A.m[8] + m[6] * A.m[9] + m[10] * A.m[10] + m[14] * A.m[11], m[2] * A.m[12] + m[6] * A.m[13] + m[10] * A.m[14] + m[14] * A.m[15], m[3] * A.m[0] + m[7] * A.m[1] + m[11] * A.m[2] + m[15] * A.m[3], // row 4
                m[3] * A.m[4] + m[7] * A.m[5] + m[11] * A.m[6] + m[15] * A.m[7], m[3] * A.m[8] + m[7] * A.m[9] + m[11] * A.m[10] + m[15] * A.m[11], m[3] * A.m[12] + m[7] * A.m[13] + m[11] * A.m[14] + m[15] * A.m[15]);
        return this;
    }

    public Vec3 multiply(Vec3 v) {
        double W = m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15];
        return new Vec3((m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12]) / W, (m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13]) / W, (m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14]) / W);
    }

    public Vec3 multiplyTranspose(Vec3 v) {
        double W = m[12] * v.x + m[13] * v.y + m[14] * v.z + m[15];
        return new Vec3((m[0] * v.x + m[1] * v.y + m[2] * v.z + m[3]) / W, (m[4] * v.x + m[5] * v.y + m[6] * v.z + m[7]) / W, (m[8] * v.x + m[9] * v.y + m[10] * v.z + m[11]) / W);
    }

    public Vec4 multiply(Vec4 v) {
        return new Vec4(m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12] * v.w, m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13] * v.w, m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14] * v.w, m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15] * v.w);
    }

    public Vec3 translation() {
        return new Vec3(m[12], m[13], m[14]);
    }

    public void setTranslation(double x, double y, double z) {
        m[12] = x;
        m[13] = y;
        m[14] = z;
    }

    public Mat4 inverse() {
        Mat4 inverse = new Mat4();
        // Cache the matrix values (makes for huge speed increases!)
        double a00 = m[0], a01 = m[1], a02 = m[2], a03 = m[3];
        double a10 = m[4], a11 = m[5], a12 = m[6], a13 = m[7];
        double a20 = m[8], a21 = m[9], a22 = m[10], a23 = m[11];
        double a30 = m[12], a31 = m[13], a32 = m[14], a33 = m[15];

        double b00 = a00 * a11 - a01 * a10;
        double b01 = a00 * a12 - a02 * a10;
        double b02 = a00 * a13 - a03 * a10;
        double b03 = a01 * a12 - a02 * a11;
        double b04 = a01 * a13 - a03 * a11;
        double b05 = a02 * a13 - a03 * a12;
        double b06 = a20 * a31 - a21 * a30;
        double b07 = a20 * a32 - a22 * a30;
        double b08 = a20 * a33 - a23 * a30;
        double b09 = a21 * a32 - a22 * a31;
        double b10 = a21 * a33 - a23 * a31;
        double b11 = a22 * a33 - a23 * a32;

        // Calculate the determinant (inlined to avoid double-caching)
        double invDet = 1 / (b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06);

        inverse.m[0] = (a11 * b11 - a12 * b10 + a13 * b09) * invDet;
        inverse.m[1] = (-a01 * b11 + a02 * b10 - a03 * b09) * invDet;
        inverse.m[2] = (a31 * b05 - a32 * b04 + a33 * b03) * invDet;
        inverse.m[3] = (-a21 * b05 + a22 * b04 - a23 * b03) * invDet;
        inverse.m[4] = (-a10 * b11 + a12 * b08 - a13 * b07) * invDet;
        inverse.m[5] = (a00 * b11 - a02 * b08 + a03 * b07) * invDet;
        inverse.m[6] = (-a30 * b05 + a32 * b02 - a33 * b01) * invDet;
        inverse.m[7] = (a20 * b05 - a22 * b02 + a23 * b01) * invDet;
        inverse.m[8] = (a10 * b10 - a11 * b08 + a13 * b06) * invDet;
        inverse.m[9] = (-a00 * b10 + a01 * b08 - a03 * b06) * invDet;
        inverse.m[10] = (a30 * b04 - a31 * b02 + a33 * b00) * invDet;
        inverse.m[11] = (-a20 * b04 + a21 * b02 - a23 * b00) * invDet;
        inverse.m[12] = (-a10 * b09 + a11 * b07 - a12 * b06) * invDet;
        inverse.m[13] = (a00 * b09 - a01 * b07 + a02 * b06) * invDet;
        inverse.m[14] = (-a30 * b03 + a31 * b01 - a32 * b00) * invDet;
        inverse.m[15] = (a20 * b03 - a21 * b01 + a22 * b00) * invDet;

        return inverse;
    }
*/
    //
    // public GL3DMat4 inverse() {
    // GL3DMat4 I = new GL3DMat4();
    //
    // // Code from Mesa-2.2\src\glu\project.c
    // double det, d12, d13, d23, d24, d34, d41;
    //
    // // Inverse = adjoint / det. (See linear algebra texts.)
    // // pre-compute 2x2 dets for last two rows when computing
    // // cofactors of first two rows.
    // d12 = (m[2] * m[7] - m[3] * m[6]);
    // d13 = (m[2] * m[11] - m[3] * m[10]);
    // d23 = (m[6] * m[11] - m[7] * m[10]);
    // d24 = (m[6] * m[15] - m[7] * m[14]);
    // d34 = (m[10] * m[15] - m[11] * m[14]);
    // d41 = (m[14] * m[3] - m[15] * m[2]);
    //
    // I.m[0] = (m[5] * d34 - m[9] * d24 + m[13] * d23);
    // I.m[1] = -(m[1] * d34 + m[9] * d41 + m[13] * d13);
    // I.m[2] = (m[1] * d24 + m[5] * d41 + m[13] * d12);
    // I.m[3] = -(m[1] * d23 - m[5] * d13 + m[9] * d12);
    //
    // // Compute determinant as early as possible using these cof_actors.
    // det = m[0] * I.m[0] + m[4] * I.m[1] + m[8] * I.m[2] + m[12] * I.m[3];
    //
    // // Run singularity test.
    // if (Math.abs(det) <= 0.0000005) {
    // // throw new IllegalArgumentException(
    // // "Matrix is singular. Inversion impossible.");
    // Log.error("Matrix is singular. Inversion impossible. Matrix:\n"+this.toString());
    // } else {
    // double invDet = 1 / det;
    // // Compute rest of inverse.
    // I.m[0] *= invDet;
    // I.m[1] *= invDet;
    // I.m[2] *= invDet;
    // I.m[3] *= invDet;
    //
    // I.m[4] = -(m[4] * d34 - m[8] * d24 + m[12] * d23) * invDet;
    // I.m[5] = (m[0] * d34 + m[8] * d41 + m[12] * d13) * invDet;
    // I.m[6] = -(m[0] * d24 + m[4] * d41 + m[12] * d12) * invDet;
    // I.m[7] = (m[0] * d23 - m[4] * d13 + m[8] * d12) * invDet;
    //
    // // Pre-compute 2x2 dets for first two rows when computing
    // // cofactors of last two rows.
    // d12 = m[0] * m[5] - m[1] * m[4];
    // d13 = m[0] * m[9] - m[1] * m[8];
    // d23 = m[4] * m[9] - m[5] * m[8];
    // d24 = m[4] * m[13] - m[5] * m[12];
    // d34 = m[8] * m[13] - m[9] * m[12];
    // d41 = m[12] * m[1] - m[13] * m[0];
    //
    // I.m[8] = (m[7] * d34 - m[11] * d24 + m[15] * d23) * invDet;
    // I.m[9] = -(m[3] * d34 + m[11] * d41 + m[15] * d13) * invDet;
    // I.m[10] = (m[3] * d24 + m[7] * d41 + m[15] * d12) * invDet;
    // I.m[11] = -(m[3] * d23 - m[7] * d13 + m[11] * d12) * invDet;
    // I.m[12] = -(m[6] * d34 - m[10] * d24 + m[14] * d23) * invDet;
    // I.m[13] = (m[2] * d34 + m[10] * d41 + m[14] * d13) * invDet;
    // I.m[14] = -(m[2] * d24 + m[6] * d41 + m[14] * d12) * invDet;
    // I.m[15] = (m[2] * d23 - m[6] * d13 + m[10] * d12) * invDet;
    // }
    // return I;
    // }
/*
    public Mat4 translate(Vec3 t) {
        m[12] += t.x;
        m[13] += t.y;
        m[14] += t.z;
        return this;
    }
*/
    public Mat4 translate(double x, double y, double z) {
        m[12] += x;
        m[13] += y;
        m[14] += z;
        return this;
    }
/*
    public static Mat4 translation(Vec3 t) {
        return new Mat4(1, 0, 0, t.x, 0, 1, 0, t.y, 0, 0, 1, t.z, 0, 0, 0, 1);
    }

    public static Mat4 translation(double x, double y, double z) {
        return new Mat4(1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1);
    }

    public Mat4 scale(Vec3 s) {
        m[0] *= s.x;
        m[5] *= s.y;
        m[10] *= s.z;
        return this;
    }

    public Mat4 scale(double sx, double sy, double sz) {
        m[0] *= sx;
        m[5] *= sy;
        m[10] *= sz;
        return this;
    }

    public static Mat4 scaling(double sx, double sy, double sz) {
        return new Mat4(sx, 0, 0, 0, 0, sy, 0, 0, 0, 0, sz, 0, 0, 0, 0, 1);
    }

    public Mat4 rotate(double angle, Vec3 axis) {
        return rotate(angle, axis.x, axis.y, axis.z);
    }

    public Mat4 rotate(double angle, double axisx, double axisy, double axisz) {
        return multiply(rotation(angle, axisx, axisy, axisz));
    }

    public Mat4 invert() {
        return set(inverse());
    }

    public Mat4 transpose() {
        double temp;
        temp = m[1];
        m[1] = m[4];
        m[4] = temp;
        temp = m[2];
        m[2] = m[8];
        m[8] = temp;
        temp = m[6];
        m[6] = m[9];
        m[9] = temp;
        temp = m[3];
        m[3] = m[12];
        m[12] = temp;
        temp = m[7];
        m[7] = m[13];
        m[13] = temp;
        temp = m[11];
        m[11] = m[14];
        m[14] = temp;
        return this;
    }

    public Mat4 swap(int i1, int i2) {
        double temp = get(i1);
        set(i1, get(i2));
        set(i2, temp);
        return this;
    }

    public static Mat4 rotation(double angle, Vec3 axis) {
        return rotation(angle, axis.x, axis.y, axis.z);
    }

    public static Mat4 rotation(double angle, double axisx, double axisy, double axisz) {
        // Quaterniond quat = new Quaterniond(degAng, axisx, axisy, axisz);
        // return buildRotationMatrix(quat);
        Mat4 r = identity();
        double ca = Math.cos(angle);
        double sa = Math.sin(angle);

        if (axisx == 1 && axisy == 0 && axisz == 0) // about x-axis
        {
            r.m[0] = 1;
            r.m[4] = 0;
            r.m[8] = 0;
            r.m[1] = 0;
            r.m[5] = ca;
            r.m[9] = -sa;
            r.m[2] = 0;
            r.m[6] = sa;
            r.m[10] = ca;
        } else if (axisx == 0 && axisy == 1 && axisz == 0) // about y-axis
        {
            r.m[0] = ca;
            r.m[4] = 0;
            r.m[8] = sa;
            r.m[1] = 0;
            r.m[5] = 1;
            r.m[9] = 0;
            r.m[2] = -sa;
            r.m[6] = 0;
            r.m[10] = ca;
        } else if (axisx == 0 && axisy == 0 && axisz == 1) // about z-axis
        {
            r.m[0] = ca;
            r.m[4] = -sa;
            r.m[8] = 0;
            r.m[1] = sa;
            r.m[5] = ca;
            r.m[9] = 0;
            r.m[2] = 0;
            r.m[6] = 0;
            r.m[10] = 1;
        } else // arbitrary axis
        {
            double len = axisx * axisx + axisy * axisy + axisz * axisz; // length
            // squared
            double x, y, z;
            x = axisx;
            y = axisy;
            z = axisz;
            if (len > 1.0001 || len < 0.9999 && len != 0) {
                len = 1 / Math.sqrt(len);
                x *= len;
                y *= len;
                z *= len;
            }
            double xy = x * y, yz = y * z, xz = x * z, xx = x * x, yy = y * y, zz = z * z;
            r.m[0] = xx + ca * (1 - xx);
            r.m[4] = xy - xy * ca - z * sa;
            r.m[8] = xz - xz * ca + y * sa;
            r.m[1] = xy - xy * ca + z * sa;
            r.m[5] = yy + ca * (1 - yy);
            r.m[9] = yz - yz * ca - x * sa;
            r.m[2] = xz - xz * ca - y * sa;
            r.m[6] = yz - yz * ca + x * sa;
            r.m[10] = zz + ca * (1 - zz);
        }
        r.m[3] = r.m[7] = r.m[11] = 0;
        r.m[15] = 1;

        return r;
    }

    public static Mat4 frustum(double l, double r, double b, double t, double n, double f) {
        return new Mat4((2 * n) / (r - l), 0, (r + l) / (r - l), 0, 0, (2 * n) / (t - b), (t + b) / (t - b), 0, 0, 0, -(f + n) / (f - n), (-2 * f * n) / (f - n), 0, 0, -1, 0);
    }

    public static Mat4 ortho(double l, double r, double b, double t, double n, double f) {
        return new Mat4(2. / (r - l), 0., 0., -(r + l) / (r - l), 0., 2 / (t - b), 0., -(t + b) / (t - b), 0., 0., -2. / (f - n), -(f + n) / (f - n), 0., 0., 0., 1.);
    }
*/
    public static Mat4 orthoInverse(double l, double r, double b, double t, double n, double f) {
        return new Mat4((r - l) * 0.5, 0., 0., -(r + l) * 0.5, 0., (t - b) * 0.5, 0., (t + b) * 0.5, 0., 0., (n - f) * 0.5, -(f + n) * 0.5, 0., 0., 0., 1.);
    }
/*
    public static Mat4 perspective(double fov, double aspect, double n, double f) {
        double t = Math.tan(Math.toRadians(fov * 0.5)) * n;
        double b = -t;
        double r = t * aspect;
        double l = -r;
        return frustum(l, r, b, t, n, f);
    }

    public static Mat4 viewport(double x, double y, double ww, double wh, double n, double f) {
        double ww2 = ww * 0.5;
        double wh2 = wh * 0.5;
        // negate the first wh because windows has topdown window coords
        return new Mat4(ww2, 0, 0, ww2 + x, 0, -wh2, 0, wh2 + y, 0, 0, (f - n) * 0.5, (f + n) * 0.5, 0, 0, 0, 1);
    }

    public Mat3 mat3() {
        return new Mat3(m[0], m[4], m[8], m[1], m[5], m[9], m[2], m[6], m[10]);
    }

    public Mat4 copy() {
        return new Mat4(this);
    }
*/
    @Override
    public String toString() {
        String format = "%01.02f";
        return String.format(format + ", ", m[0]) +
               String.format(format + ", ", m[4]) +
               String.format(format + ", ", m[8]) +
               String.format(format + ", \n", m[12]) +
               String.format(format + ", ", m[1]) +
               String.format(format + ", ", m[5]) +
               String.format(format + ", ", m[9]) +
               String.format(format + ", \n", m[13]) +
               String.format(format + ", ", m[2]) +
               String.format(format + ", ", m[6]) +
               String.format(format + ", ", m[10]) +
               String.format(format + ", \n", m[14]) +
               String.format(format + ", ", m[3]) +
               String.format(format + ", ", m[7]) +
               String.format(format + ", ", m[11]) +
               String.format(format + ", \n", m[15]);
    }

}
