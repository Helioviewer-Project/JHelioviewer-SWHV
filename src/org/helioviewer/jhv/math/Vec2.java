package org.helioviewer.jhv.math;

import java.util.Objects;

public class Vec2 {

    public static final Vec2 NAN_VECTOR = new Vec2(Double.NaN, Double.NaN);

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
        return o instanceof Vec2 && isApproxEqual((Vec2) o, 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ',' + y + ')';
    }

}
