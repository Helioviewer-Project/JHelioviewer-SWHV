package org.helioviewer.jhv.math;

public final class Mat2 {

    public static final Mat2 IDENTITY = new Mat2(1, 0, 0, 1);

    public final double m00;
    public final double m01;
    public final double m10;
    public final double m11;

    public Mat2(double _m00, double _m01, double _m10, double _m11) {
        m00 = _m00;
        m01 = _m01;
        m10 = _m10;
        m11 = _m11;
    }

    public static Mat2 rotation(double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new Mat2(c, -s, s, c);
    }

    public Mat2 inverse() {
        double determinant = m00 * m11 - m01 * m10;
        if (!Double.isFinite(determinant) || determinant == 0)
            return IDENTITY;
        return new Mat2(m11 / determinant, -m01 / determinant, -m10 / determinant, m00 / determinant);
    }

    public Vec2 transform(double x, double y) {
        return new Vec2(m00 * x + m01 * y, m10 * x + m11 * y);
    }

}
