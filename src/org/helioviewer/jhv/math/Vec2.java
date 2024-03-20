package org.helioviewer.jhv.math;

public class Vec2 {

    public static final Vec2 ZERO = new Vec2(0, 0);
    public static final Vec2 NAN = new Vec2(Double.NaN, Double.NaN);

    public double x;
    public double y;

    public Vec2(double _x, double _y) {
        x = _x;
        y = _y;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof Vec2 v)
            return Double.doubleToLongBits(x) == Double.doubleToLongBits(v.x) &&
                    Double.doubleToLongBits(y) == Double.doubleToLongBits(v.y);
        return false;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(x);
        return 31 * result + Double.hashCode(y);
    }

    @Override
    public String toString() {
        return "(" + x + ',' + y + ')';
    }

}
