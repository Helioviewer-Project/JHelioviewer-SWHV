package org.helioviewer.jhv.math;

public class Vec2 {

    public static final Vec2 NAN = new Vec2(Double.NaN, Double.NaN);

    public double x;
    public double y;

    public Vec2(double _x, double _y) {
        x = _x;
        y = _y;
    }

    private boolean isApproxEqual(Vec2 vec, double tolerance) {
        return Math.abs(x - vec.x) <= tolerance && Math.abs(y - vec.y) <= tolerance;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Vec2))
            return false;
        Vec2 v = (Vec2) o;
        return Double.doubleToLongBits(x) == Double.doubleToLongBits(v.x) &&
               Double.doubleToLongBits(y) == Double.doubleToLongBits(v.y);
    }

    @Override
    public int hashCode() {
        int result = 1;
        long tmp = Double.doubleToLongBits(x);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        tmp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "(" + x + ',' + y + ')';
    }

}
