package org.helioviewer.jhv.plugins.eveplugin.draw;

public class TimeAxis {

    public long min;
    public long max;

    public TimeAxis(long _min, long _max) {
        min = _min;
        max = _max;
    }

    public double getRatio(double size) {
        return size / (max - min);
    }

    public int value2pixel(int x0, int w, long val) {
        return (int) ((double) w * (val - min) / (max - min) + x0);
    }

    public long pixel2value(int x0, int w, int x) {
        return (long) (min + (max - min) * (x - x0) / (double) w);
    }

    public void move(int x0, int w, double pixelDistance) {
        double diff = (double) (max - min) / w;
        min = (long) (min + pixelDistance * diff);
        max = (long) (max + pixelDistance * diff);
    }

    public void zoom(int x0, int w, int x, double factor) {
        double multiplier = (max - min) * factor / w;
        final double ratio = (x - x0) / (double) w;
        min = (long) (min - multiplier * ratio);
        max = (long) (max + multiplier * (1. - ratio));
    }

}
