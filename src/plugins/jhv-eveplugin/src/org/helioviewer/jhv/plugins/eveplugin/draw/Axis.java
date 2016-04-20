package org.helioviewer.jhv.plugins.eveplugin.draw;

public class Axis {

    public double min;
    public double max;

    public Axis(double _min, double _max) {
        min = _min;
        max = _max;
    }

    public int calculateLocation(double value, double size, double start) {
        return (int) (((value - min) * getRatio(size)) + start);
    }

    public double getRatio(double size) {
        return size / (max - min);
    }

    public void move(double scaledDistance) {
        double diff = max - min;
        min = Math.floor(min + scaledDistance * diff);
        max = Math.floor(max + scaledDistance * diff);
    }

    public void zoom(int x, int w, int x0, double factor) {
        double diff = max - min;
        final double ratio = (x - x0) / (double) w;
        min = Math.floor(min - factor / w * diff * ratio);
        max = Math.floor(max + factor / w * diff * (1. - ratio));
    }

}
