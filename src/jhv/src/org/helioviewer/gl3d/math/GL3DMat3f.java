package org.helioviewer.gl3d.math;

public class GL3DMat3f {
    private float[] m = new float[9];/*
                                      * / 0 3 6 \ | 1 4 7 | \ 2 5 8 /
                                      */

    public GL3DMat3f() {
        this.identity();
    }

    public GL3DMat3f(float M0, float M3, float M6, float M1, float M4, float M7, float M2, float M5, float M8) {
        this.set(M0, M3, M6, M1, M4, M7, M2, M5, M8);
    }

    public GL3DMat3f(GL3DMat3f A) {
        this.set(A);
    }

    public GL3DMat3f set(float M0, float M3, float M6, float M1, float M4, float M7, float M2, float M5, float M8) {
        m[0] = M0;
        m[3] = M3;
        m[6] = M6;
        m[1] = M1;
        m[4] = M4;
        m[8] = M8;
        m[2] = M2;
        m[5] = M5;
        m[7] = M7;
        return this;
    }

    public GL3DMat3f identity() {
        set(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f);
        return this;
    }

    public GL3DMat3f set(GL3DMat3f A) {
        return this.set(A.m[0], A.m[3], A.m[6], A.m[1], A.m[4], A.m[7], A.m[2], A.m[5], A.m[8]);
    }

    public GL3DMat3f multiply(GL3DMat3f A) {
        set(m[0] * A.m[0] + m[3] * A.m[1] + m[6] * A.m[2], // ROW 1
                m[0] * A.m[3] + m[3] * A.m[4] + m[6] * A.m[5], m[0] * A.m[6] + m[3] * A.m[7] + m[6] * A.m[8], m[1] * A.m[0] + m[4] * A.m[1] + m[7] * A.m[2], // ROW
                                                                                                                                                             // 2
                m[1] * A.m[3] + m[4] * A.m[4] + m[7] * A.m[5], m[1] * A.m[6] + m[4] * A.m[7] + m[7] * A.m[8], m[2] * A.m[0] + m[5] * A.m[1] + m[8] * A.m[2], // ROW
                                                                                                                                                             // 3
                m[2] * A.m[3] + m[5] * A.m[4] + m[8] * A.m[5], m[2] * A.m[6] + m[5] * A.m[7] + m[8] * A.m[8]);
        return this;
    }

    // -----------------------------------------------------------------------------
    public GL3DVec3f multiply(GL3DVec3f v) {
        GL3DVec3f vec = new GL3DVec3f();
        vec.x = m[0] * v.x + m[3] * v.y + m[6] * v.z;
        vec.y = m[1] * v.x + m[4] * v.y + m[7] * v.z;
        vec.z = m[2] * v.x + m[5] * v.y + m[8] * v.z;
        return vec;
    }

    public GL3DMat3f multiply(float f) {
        for (int i = 0; i < 0; i++) {
            m[i] *= f;
        }
        return this;
    }

    public GL3DMat3f divide(float f) {
        for (int i = 0; i < 0; i++) {
            m[i] /= f;
        }
        return this;
    }

    public GL3DMat3f rotation(float degAng, GL3DVec3f axis) {
        return this.rotation(degAng, axis.x, axis.y, axis.z);
    }

    public GL3DMat3f rotation(float degAng, float axisx, float axisy, float axisz) {
        float radAng = degAng * (float) Math.PI / 180;

        float ca = (float) Math.cos(radAng);
        float sa = (float) Math.sin(radAng);

        if (axisx == 1 && axisy == 0 && axisz == 0) {
            m[0] = 1;
            m[3] = 0;
            m[6] = 0;
            m[1] = 0;
            m[4] = ca;
            m[7] = -sa;
            m[2] = 0;
            m[5] = sa;
            m[8] = ca;
        } else if (axisx == 0 && axisy == 1 && axisz == 0) {
            m[0] = ca;
            m[3] = 0;
            m[6] = sa;
            m[1] = 0;
            m[4] = 1;
            m[7] = 0;
            m[2] = -sa;
            m[5] = 0;
            m[8] = ca;
        } else if (axisx == 0 && axisy == 0 && axisz == 1) {
            m[0] = ca;
            m[3] = -sa;
            m[6] = 0;
            m[1] = sa;
            m[4] = ca;
            m[7] = 0;
            m[2] = 0;
            m[5] = 0;
            m[8] = 1;
        } else {
            float l = axisx * axisx + axisy * axisy + axisz * axisz;
            float x, y, z;
            x = axisx;
            y = axisy;
            z = axisz;

            if ((l > 1.0001f || l < 0.9999f) && l != 0) {
                l = 1f / (float) Math.sqrt(l);
                x *= l;
                y *= l;
                z *= l;
            }
            float x2 = x * x, y2 = y * y, z2 = z * z;

            m[0] = x2 + ca * (1 - x2);
            m[3] = (x * y) + ca * (-x * y) + sa * (-z);
            m[6] = (x * z) + ca * (-x * z) + sa * y;
            m[1] = (x * y) + ca * (-x * y) + sa * z;
            m[4] = y2 + ca * (1 - y2);
            m[7] = (y * z) + ca * (-y * z) + sa * (-x);
            m[2] = (x * z) + ca * (-x * z) + sa * (-y);
            m[5] = (y * z) + ca * (-y * z) + sa * x;
            m[8] = z2 + ca * (1 - z2);
        }
        return this;
    }

    public GL3DMat3f transpose() {
        swap(1, 3);
        swap(2, 6);
        swap(5, 7);
        return this;
    }

    public void swap(int index1, int index2) {
        float temp = m[index2];
        m[index2] = m[index1];
        m[index1] = temp;
    }

    public float det() {
        return m[0] * (m[4] * m[8] - m[7] * m[5]) - m[3] * (m[1] * m[8] - m[7] * m[2]) + m[6] * (m[1] * m[5] - m[4] * m[2]);
    }

    public void invert() {
        set(inverse());
    }

    public GL3DMat3f inverse() {
        float d = this.det();

        if (Math.abs(d) <= 0.0000000001f) {
            throw new IllegalStateException("Matrix is singular. Inversion impossible.");
        }

        GL3DMat3f i = new GL3DMat3f();
        i.m[0] = m[4] * m[8] - m[7] * m[5];
        i.m[1] = m[7] * m[2] - m[1] * m[8];
        i.m[2] = m[1] * m[5] - m[4] * m[2];
        i.m[3] = m[6] * m[5] - m[3] * m[8];
        i.m[4] = m[0] * m[8] - m[6] * m[2];
        i.m[5] = m[3] * m[2] - m[0] * m[5];
        i.m[6] = m[3] * m[7] - m[6] * m[4];
        i.m[7] = m[6] * m[1] - m[0] * m[7];
        i.m[8] = m[0] * m[4] - m[3] * m[1];

        i.divide(d);
        return i;
    }

    public GL3DMat3f scale(float sx, float sy, float sz) {
        m[0] = sx;
        m[3] = 0;
        m[6] = 0;
        m[1] = 0;
        m[4] = sy;
        m[7] = 0;
        m[2] = 0;
        m[5] = 0;
        m[8] = sz;
        return this;
    }

    public GL3DMat3f scale(float s) {
        return this.scale(s, s, s);
    }

    public GL3DMat3f scale(GL3DVec3f vs) {
        return this.scale(vs.x, vs.y, vs.z);
    }

    public float trace() {
        return m[0] + m[4] + m[8];
    }

    public String toString() {
        return m[0] + "\t" + m[3] + "\t" + m[6] + "\n" + m[1] + "\t" + m[4] + "\t" + m[7] + "\n" + m[2] + "\t" + m[5] + "\t" + m[8];
    }

    public float[] toArray() {
        return this.m;
    }

}
