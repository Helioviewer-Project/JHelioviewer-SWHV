package org.helioviewer.jhv.base;

public class Range {

    public double min = Double.MAX_VALUE;
    public double max = Double.MIN_VALUE;

    public Range() {
        this(Double.MAX_VALUE, Double.MIN_VALUE);
    }

    public Range(final double _min, final double _max) {
        min = _min;
        max = _max;
    }

    public Range(final Range other) {
        min = other.min;
        max = other.max;
    }

    public void reset() {
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
    }

    public boolean setMin(final double _min) {
        if (min > _min) {
            min = _min;
            return true;
        }
        return false;
    }

    public boolean setMax(final double _max) {
        if (max < _max) {
            max = _max;
            return true;
        }
        return false;
    }

    public void setMinMax(final double value) {
        min = min < value ? min : value;
        max = max > value ? max : value;
    }

    @Override
    public String toString() {
        return "Range: [" + min + "," + max + "]";
    }

}
