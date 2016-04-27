package org.helioviewer.jhv.plugins.eveplugin.draw;


public class TimeAxis {

    public long start;
    public long end;

    public TimeAxis(long _min, long _max) {
        start = _min;
        end = _max;
    }

    public double getRatio(double size) {
        return size / (end - start);
    }

    public int value2pixel(int x0, int w, long val) {
        return (int) ((double) w * (val - start) / (end - start) + x0);
    }

    public long pixel2value(int x0, int w, int x) {
        return (long) (start + (end - start) * (x - x0) / (double) w);
    }

    void move(int x0, int w, double pixelDistance) {
        double diff = (double) (end - start) / w;
        start = (long) (start + pixelDistance * diff);
        end = (long) (end + pixelDistance * diff);
    }

    void zoom(int x0, int w, int x, double factor) {
        double multiplier = (end - start) * factor / w;
        double ratio = (x - x0) / (double) w;
        start = (long) (start - multiplier * ratio);
        end = (long) (end + multiplier * (1. - ratio));
    }

    void set(long _start, long _end) {
        start = _start;
        end = _end;
    }

}
