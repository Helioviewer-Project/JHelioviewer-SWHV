package org.helioviewer.jhv.math;

public class Mat4f {

    //  0 4 8 12 1 5 9 13 2 6 10 14 3 7 11 15
    private static void set(float[] m, float M0, float M4, float M8, float M12, float M1, float M5, float M9, float M13, float M2, float M6, float M10, float M14, float M3, float M7, float M11, float M15) {
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

    public static void orthoSymmetricInverse(float[] m, float width, float height, float near, float far) {
        set(m, width * 0.5f, 0, 0, 0, 0, height * 0.5f, 0, 0, 0, 0, (near - far) * 0.5f, -(near + far) * 0.5f, 0, 0, 0, 1);
    }

    public static void translate(float[] m, float x, float y, float z) {
        m[12] += x;
        m[13] += y;
        m[14] += z;
    }

}
