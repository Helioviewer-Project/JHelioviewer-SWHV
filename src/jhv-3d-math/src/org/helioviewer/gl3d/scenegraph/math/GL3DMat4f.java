package org.helioviewer.gl3d.scenegraph.math;

public class GL3DMat4f {
    /**
     * 0 4 8 12 1 5 9 13 2 6 10 14 3 7 11 15
     */
    public float[] m = new float[16];

    public GL3DMat4f(float M0, float M4, float M8, float M12, float M1, float M5, float M9, float M13, float M2, float M6, float M10, float M14, float M3, float M7, float M11, float M15) {
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

    public GL3DMat4f(float[] M) {
        m[0] = M[0];
        m[4] = M[4];
        m[8] = M[8];
        m[12] = M[12];
        m[1] = M[1];
        m[5] = M[5];
        m[9] = M[9];
        m[13] = M[13];
        m[2] = M[2];
        m[6] = M[6];
        m[10] = M[10];
        m[14] = M[14];
        m[3] = M[3];
        m[7] = M[7];
        m[11] = M[11];
        m[15] = M[15];
    }

    public GL3DMat4f() {
    }

    public GL3DMat4f(GL3DMat4f mat) {
        set(mat);
    }

    public GL3DMat4f copy() {
        return new GL3DMat4f(this);
    }

    public void setIdentity() {
        this.set(GL3DMat4f.identity());
    }

    public static GL3DMat4f identity() {
        return new GL3DMat4f(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f);
    }

    public GL3DMat4f set(GL3DMat4f A) {
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

    public GL3DMat4f set(float M0, float M4, float M8, float M12, float M1, float M5, float M9, float M13, float M2, float M6, float M10, float M14, float M3, float M7, float M11, float M15) {
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

    public GL3DMat4f set(int index, float f) {
        if (index < 0 || index > 15)
            throw new IndexOutOfBoundsException("Mat4 has 16 fields");

        m[index] = f;
        return this;
    }

    public float get(int index) {
        if (index < 0 || index > 15)
            throw new IndexOutOfBoundsException("Mat4 has 16 fields");

        return m[index];
    }

    public GL3DMat4f multiply(GL3DMat4f A) {
        set(m[0] * A.m[0] + m[4] * A.m[1] + m[8] * A.m[2] + m[12] * A.m[3], // row
                                                                            // 1
                m[0] * A.m[4] + m[4] * A.m[5] + m[8] * A.m[6] + m[12] * A.m[7], m[0] * A.m[8] + m[4] * A.m[9] + m[8] * A.m[10] + m[12] * A.m[11], m[0] * A.m[12] + m[4] * A.m[13] + m[8] * A.m[14] + m[12] * A.m[15],

                m[1] * A.m[0] + m[5] * A.m[1] + m[9] * A.m[2] + m[13] * A.m[3], // row
                                                                                // 2
                m[1] * A.m[4] + m[5] * A.m[5] + m[9] * A.m[6] + m[13] * A.m[7], m[1] * A.m[8] + m[5] * A.m[9] + m[9] * A.m[10] + m[13] * A.m[11], m[1] * A.m[12] + m[5] * A.m[13] + m[9] * A.m[14] + m[13] * A.m[15],

                m[2] * A.m[0] + m[6] * A.m[1] + m[10] * A.m[2] + m[14] * A.m[3], // row
                                                                                 // 3
                m[2] * A.m[4] + m[6] * A.m[5] + m[10] * A.m[6] + m[14] * A.m[7], m[2] * A.m[8] + m[6] * A.m[9] + m[10] * A.m[10] + m[14] * A.m[11], m[2] * A.m[12] + m[6] * A.m[13] + m[10] * A.m[14] + m[14] * A.m[15],

                m[3] * A.m[0] + m[7] * A.m[1] + m[11] * A.m[2] + m[15] * A.m[3], // row
                                                                                 // 4
                m[3] * A.m[4] + m[7] * A.m[5] + m[11] * A.m[6] + m[15] * A.m[7], m[3] * A.m[8] + m[7] * A.m[9] + m[11] * A.m[10] + m[15] * A.m[11], m[3] * A.m[12] + m[7] * A.m[13] + m[11] * A.m[14] + m[15] * A.m[15]);
        return this;
    }

    public GL3DVec3f multiply(GL3DVec3f v) {
        float W = m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15];
        return new GL3DVec3f((m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12]) / W, (m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13]) / W, (m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14]) / W);
    }

    public GL3DVec4f multiply(GL3DVec4f v) {
        GL3DVec4f newV = new GL3DVec4f(m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12] * v.w, m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13] * v.w, m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14] * v.w, m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15] * v.w);
        return newV;
    }

    public GL3DVec3f translation() {
        return new GL3DVec3f(m[12], m[13], m[14]);
    }

    public GL3DMat4f inverse() {
        GL3DMat4f I = new GL3DMat4f();

        // Code from Mesa-2.2\src\glu\project.c
        float det, d12, d13, d23, d24, d34, d41;

        // Inverse = adjoint / det. (See linear algebra texts.)
        // pre-compute 2x2 dets for last two rows when computing
        // cof_actors of first two rows.
        d12 = (m[2] * m[7] - m[3] * m[6]);
        d13 = (m[2] * m[11] - m[3] * m[10]);
        d23 = (m[6] * m[11] - m[7] * m[10]);
        d24 = (m[6] * m[15] - m[7] * m[14]);
        d34 = (m[10] * m[15] - m[11] * m[14]);
        d41 = (m[14] * m[3] - m[15] * m[2]);

        I.m[0] = (m[5] * d34 - m[9] * d24 + m[13] * d23);
        I.m[1] = -(m[1] * d34 + m[9] * d41 + m[13] * d13);
        I.m[2] = (m[1] * d24 + m[5] * d41 + m[13] * d12);
        I.m[3] = -(m[1] * d23 - m[5] * d13 + m[9] * d12);

        // Compute determinant as early as possible using these cof_actors.
        det = m[0] * I.m[0] + m[4] * I.m[1] + m[8] * I.m[2] + m[12] * I.m[3];

        // Run singularity test.
        if (Math.abs(det) <= 0.00005) {
            throw new IllegalArgumentException("Matrix is singular. Inversion impossible.");
        } else {
            float invDet = 1 / det;
            // Compute rest of inverse.
            I.m[0] *= invDet;
            I.m[1] *= invDet;
            I.m[2] *= invDet;
            I.m[3] *= invDet;

            I.m[4] = -(m[4] * d34 - m[8] * d24 + m[12] * d23) * invDet;
            I.m[5] = (m[0] * d34 + m[8] * d41 + m[12] * d13) * invDet;
            I.m[6] = -(m[0] * d24 + m[4] * d41 + m[12] * d12) * invDet;
            I.m[7] = (m[0] * d23 - m[4] * d13 + m[8] * d12) * invDet;

            // Pre-compute 2x2 dets for first two rows when computing
            // cofactors of last two rows.
            d12 = m[0] * m[5] - m[1] * m[4];
            d13 = m[0] * m[9] - m[1] * m[8];
            d23 = m[4] * m[9] - m[5] * m[8];
            d24 = m[4] * m[13] - m[5] * m[12];
            d34 = m[8] * m[13] - m[9] * m[12];
            d41 = m[12] * m[1] - m[13] * m[0];

            I.m[8] = (m[7] * d34 - m[11] * d24 + m[15] * d23) * invDet;
            I.m[9] = -(m[3] * d34 + m[11] * d41 + m[15] * d13) * invDet;
            I.m[10] = (m[3] * d24 + m[7] * d41 + m[15] * d12) * invDet;
            I.m[11] = -(m[3] * d23 - m[7] * d13 + m[11] * d12) * invDet;
            I.m[12] = -(m[6] * d34 - m[10] * d24 + m[14] * d23) * invDet;
            I.m[13] = (m[2] * d34 + m[10] * d41 + m[14] * d13) * invDet;
            I.m[14] = -(m[2] * d24 + m[6] * d41 + m[14] * d12) * invDet;
            I.m[15] = (m[2] * d23 - m[6] * d13 + m[10] * d12) * invDet;
        }
        return I;
    }

    public GL3DMat4f translate(GL3DVec3f t) {
        return this.multiply(GL3DMat4f.translation(t));
    }

    public GL3DMat4f translate(float x, float y, float z) {
        return this.multiply(GL3DMat4f.translation(new GL3DVec3f(x, y, z)));
    }

    public GL3DMat4f rotate(float degAng, GL3DVec3f axis) {
        return this.rotate(degAng, axis.x, axis.y, axis.z);
    }

    public GL3DMat4f rotate(float degAng, float axisx, float axisy, float axisz) {
        return this.multiply(GL3DMat4f.rotation(degAng, axisx, axisy, axisz));
    }

    public GL3DMat4f scale(GL3DVec3f s) {
        return this.scale(s.x, s.y, s.z);
    }

    public GL3DMat4f scale(float sx, float sy, float sz) {
        return this.multiply(GL3DMat4f.scaling(sx, sy, sz));
    }

    public GL3DMat4f invert() {
        return this.set(this.inverse());
    }

    public GL3DMat4f transpose() {
        return swap(1, 4).swap(2, 8).swap(6, 9).swap(3, 12).swap(7, 13).swap(11, 14);
    }

    public GL3DMat4f swap(int i1, int i2) {
        float temp = get(i1);
        set(i1, get(i2));
        set(i2, temp);
        return this;
    }

    public void posAtUp(GL3DVec3f pos) {
        this.posAtUp(pos, new GL3DVec3f(), new GL3DVec3f());
    }

    public void posAtUp(GL3DVec3f pos, GL3DVec3f dirAt, GL3DVec3f dirUp) {
        lightAt(pos, dirAt, dirUp);
    }

    public void lightAt(GL3DVec3f pos, GL3DVec3f dirAt, GL3DVec3f dirUp) {
        GL3DVec3f VX = new GL3DVec3f();
        GL3DVec3f VY = new GL3DVec3f();
        GL3DVec3f VZ;
        // GL3DVec3f VT = new GL3DVec3f();

        GL3DMat3f xz = new GL3DMat3f(0f, 0f, 1f, 0f, 0f, 0f, -1f, 0f, 0f);

        VZ = GL3DVec3f.subtract(pos, dirAt);
        if (dirUp.isApproxEqual(GL3DVec3f.ZERO, 0f)) {
            VX = xz.multiply(VZ);
            VX.normalize();
            VY = GL3DVec3f.cross(VZ, VX);
            VY.normalize();
        } else {
            VX = GL3DVec3f.cross(dirUp, VZ);
            VX.normalize();
            VY = GL3DVec3f.cross(VZ, VX);
            VY.normalize();
        }

        set(VX.x, VY.x, VZ.x, pos.x, VX.y, VY.y, VZ.y, pos.y, VX.z, VY.z, VZ.z, pos.z, 0f, 0f, 0f, 1f);
    }

    public static GL3DMat4f translation(GL3DVec3f t) {
        GL3DMat4f tr = GL3DMat4f.identity();
        tr.set(12, t.x);
        tr.set(13, t.y);
        tr.set(14, t.z);
        return tr;
    }

    public static GL3DMat4f scaling(float sx, float sy, float sz) {
        GL3DMat4f s = GL3DMat4f.identity();
        s.set(0, sx);
        s.set(5, sy);
        s.set(10, sy);
        return s;
    }

    public static GL3DMat4f rotation(float degAng, float axisx, float axisy, float axisz) {
        GL3DMat4f r = GL3DMat4f.identity();
        float RadAng = (float) Math.toRadians(degAng);
        float ca = (float) Math.cos(RadAng);
        float sa = (float) Math.sin(RadAng);

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
            float len = axisx * axisx + axisy * axisy + axisz * axisz; // length
                                                                       // squared
            float x, y, z;
            x = axisx;
            y = axisy;
            z = axisz;
            if (len > 1.0001 || len < 0.9999 && len != 0) {
                len = 1 / (float) Math.sqrt(len);
                x *= len;
                y *= len;
                z *= len;
            }
            float xy = x * y, yz = y * z, xz = x * z, xx = x * x, yy = y * y, zz = z * z;
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

    public static GL3DMat4f frustum(float l, float r, float b, float t, float n, float f) {
        return new GL3DMat4f((2 * n) / (r - l), 0f, (r + l) / (r - l), 0f, 0f, (2 * n) / (t - b), (t + b) / (t - b), 0f, 0f, 0f, -(f + n) / (f - n), (-2 * f * n) / (f - n), 0f, 0f, -1f, 0f);
    }

    public static GL3DMat4f perspective(float fov, float aspect, float n, float f) {
        float t = (float) (Math.tan(Math.toRadians(fov * 0.5)) * n);
        float b = -t;
        float r = t * aspect;
        float l = -r;
        return frustum(l, r, b, t, n, f);
    }

    public static GL3DMat4f viewport(float x, float y, float ww, float wh, float n, float f) {
        float ww2 = ww * 0.5f;
        float wh2 = wh * 0.5f;
        // negate the first wh because windows has topdown window coords
        return new GL3DMat4f(ww2, 0f, 0f, ww2 + x, 0f, -wh2, 0f, wh2 + y, 0f, 0f, (f - n) * 0.5f, (f + n) * 0.5f, 0f, 0f, 0f, 1f);
    }

    public GL3DMat3d mat3() {
        GL3DMat3d mat3 = new GL3DMat3d(m[0], m[4], m[8], m[1], m[5], m[9], m[2], m[6], m[10]);
        return mat3;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String format = "%01.02f";
        sb.append(String.format(format, m[0]) + ", ");
        sb.append(String.format(format, m[4]) + ", ");
        sb.append(String.format(format, m[8]) + ", ");
        sb.append(String.format(format, m[12]) + ", \n");
        sb.append(String.format(format, m[1]) + ", ");
        sb.append(String.format(format, m[5]) + ", ");
        sb.append(String.format(format, m[9]) + ", ");
        sb.append(String.format(format, m[13]) + ", \n");
        sb.append(String.format(format, m[2]) + ", ");
        sb.append(String.format(format, m[6]) + ", ");
        sb.append(String.format(format, m[10]) + ", ");
        sb.append(String.format(format, m[14]) + ", \n");
        sb.append(String.format(format, m[3]) + ", ");
        sb.append(String.format(format, m[7]) + ", ");
        sb.append(String.format(format, m[11]) + ", ");
        sb.append(String.format(format, m[15]) + ", \n");

        return sb.toString();
    }

}
