package org.helioviewer.jhv.plugins.eveplugin.draw;

import org.helioviewer.jhv.base.time.TimeUtils;

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
        move(pixelDistance * diff);
    }

    void move(double diff) {
        start = (long) (start + diff);
        end = (long) (end + diff);
        adaptBounds();
    }

    void zoom(int x0, int w, int x, double factor) {
        double multiplier = (end - start) * factor / w;
        double ratio = (x - x0) / (double) w;
        start = (long) (start - multiplier * ratio);
        end = (long) (end + multiplier * (1. - ratio));
        adaptBounds();
    }

    void set(long _start, long _end, boolean needAdapt) {
        start = _start;
        end = _end;
        if (needAdapt)
            adaptBounds();
    }

    private void adaptBounds() {
        long now = System.currentTimeMillis();
        long intervalLength = Math.min(end - start, now - TimeUtils.MINIMAL_DATE.milli);
        if (intervalLength == 0) {
            end = start + TimeUtils.MINUTE_IN_MILLIS;
            intervalLength = TimeUtils.MINUTE_IN_MILLIS;
        }
        if (end > now) {
            start = now - intervalLength;
            end = now;
        }
        if (start < TimeUtils.MINIMAL_DATE.milli) {
            start = TimeUtils.MINIMAL_DATE.milli;
            end = TimeUtils.MINIMAL_DATE.milli + intervalLength;
        }
    }
}
