package org.helioviewer.jhv.astronomy;

// Reimplementation of some basic SPICE functions: threadsafe and avoids copies between Java and C
class SpiceMath {

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

    static double[] recrad(double[] rectan) {
        double[] ret = reclat(rectan);
        if (ret[1] < 0.) {
            ret[1] += Math.TAU;
        }
        return ret;
    }

    static double[] latrec(double radius, double lon, double lat) { // same as radrec
        return new double[]{
                radius * Math.cos(lon) * Math.cos(lat),
                radius * Math.sin(lon) * Math.cos(lat),
                radius * Math.sin(lat)};
    }

    static double[] mxv(double[][] m, double[] v) {
        return new double[]{
                m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
                m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
                m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]};
    }

}
