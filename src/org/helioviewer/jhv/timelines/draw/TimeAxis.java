package org.helioviewer.jhv.timelines.draw;

import org.helioviewer.jhv.time.TimeUtils;

public class TimeAxis {

    private long start;
    private long end;

    public TimeAxis(long _min, long _max) {
        start = _min;
        end = _max;
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    public int value2pixel(int x0, int w, long val) {
        return (int) ((double) w * (val - start) / (end - start) + x0);
    }

    public long pixel2value(int x0, int w, int x) {
        return (long) (start + (end - start) * (x - x0) / (double) w);
    }

    void move(int w, double pixelDistance) {
        long diff = (long) (pixelDistance * (end - start) / w);
        move(diff);
    }

    void move(long diff) {
        start += diff;
        end += diff;
        adaptBounds();
    }

    void zoom(int x0, int w, int x, double factor) {
        double multiplier = factor * (end - start) / w;
        double ratio = (x - x0) / (double) w;
        start -= (long) (multiplier * ratio);
        end += (long) (multiplier * (1. - ratio));
        adaptBounds();
    }

    void set(long _start, long _end) {
        start = _start;
        end = _end;
        adaptBounds();
    }

    private void adaptBounds() {
        long now = System.currentTimeMillis();
        long intervalLength = Math.min(end - start, now - TimeUtils.MINIMAL_TIME.milli);
        if (intervalLength < TimeUtils.MINUTE_IN_MILLIS) { // or intervalLength <= 0, via wheel zoomin
            end = start + TimeUtils.MINUTE_IN_MILLIS;
            intervalLength = TimeUtils.MINUTE_IN_MILLIS;
        }
        if (end > now) {
            start = now - intervalLength;
            end = now;
        }
        if (start < TimeUtils.MINIMAL_TIME.milli) {
            start = TimeUtils.MINIMAL_TIME.milli;
            end = TimeUtils.MINIMAL_TIME.milli + intervalLength;
        }
    }

}
