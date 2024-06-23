package org.helioviewer.jhv.astronomy;

import org.helioviewer.jhv.math.Quat;

// Reimplementation of some basic SPICE functions: threadsafe and avoids copies between Java and C
public class SpiceMath {

    static double[] reclat(double[] rectan) {
        double big, d1, d2;
        d1 = Math.abs(rectan[0]);
        d2 = Math.abs(rectan[1]);
        d1 = Math.max(d1, d2);
        d2 = Math.abs(rectan[2]);
        big = Math.max(d1, d2);

        if (big > 0.) {
            double x = rectan[0] / big;
            double y = rectan[1] / big;
            double z = rectan[2] / big;
            double lon = x == 0. && y == 0. ? 0. : Math.atan2(y, x);

            return new double[]{
                    big * Math.sqrt(x * x + y * y + z * z),
                    lon,
                    Math.atan2(z, Math.sqrt(x * x + y * y))};
        } else {
            return new double[3];
        }
    }

    public static double[] recrad(double[] rectan) {
        double[] ret = reclat(rectan);
        if (ret[1] < 0.) {
            ret[1] += Math.TAU;
        }
        return ret;
    }

    public static double[] latrec(double radius, double lon, double lat) { // same as radrec
        double clat = Math.cos(lat);
        return new double[]{
                radius * Math.cos(lon) * clat,
                radius * Math.sin(lon) * clat,
                radius * Math.sin(lat)};
    }

    public static double[] mxv(double[][] m, double[] v) {
        return new double[]{
                m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
                m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
                m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]};
    }

    public static double[] mtxv(double[][] m, double[] v) {
        return new double[]{
                m[0][0] * v[0] + m[1][0] * v[1] + m[2][0] * v[2],
                m[0][1] * v[0] + m[1][1] * v[1] + m[2][1] * v[2],
                m[0][2] * v[0] + m[1][2] * v[1] + m[2][2] * v[2]};
    }

    // https://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/
    static Quat m2q(double[][] m) {
        double w, x, y, z;
        double trace = m[0][0] + m[1][1] + m[2][2];
        if (trace > 0) {
            double s = 0.5 / Math.sqrt(trace + 1.0);
            w = 0.25 / s;
            x = (m[2][1] - m[1][2]) * s;
            y = (m[0][2] - m[2][0]) * s;
            z = (m[1][0] - m[0][1]) * s;
        } else {
            if (m[0][0] > m[1][1] && m[0][0] > m[2][2]) {
                double s = 0.5 / Math.sqrt(1.0 + m[0][0] - m[1][1] - m[2][2]);
                w = (m[2][1] - m[1][2]) * s;
                x = 0.25 / s;
                y = (m[0][1] + m[1][0]) * s;
                z = (m[0][2] + m[2][0]) * s;
            } else if (m[1][1] > m[2][2]) {
                double s = 0.5 / Math.sqrt(1.0 + m[1][1] - m[0][0] - m[2][2]);
                w = (m[0][2] - m[2][0]) * s;
                x = (m[0][1] + m[1][0]) * s;
                y = 0.25 / s;
                z = (m[1][2] + m[2][1]) * s;
            } else {
                double s = 0.5 / Math.sqrt(1.0 + m[2][2] - m[0][0] - m[1][1]);
                w = (m[1][0] - m[0][1]) * s;
                x = (m[0][2] + m[2][0]) * s;
                y = (m[1][2] + m[2][1]) * s;
                z = 0.25 / s;
            }
        }
        return new Quat(w, x, y, z);
    }

}
