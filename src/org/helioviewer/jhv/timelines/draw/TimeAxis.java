package org.helioviewer.jhv.timelines.draw;

import org.helioviewer.jhv.time.TimeUtils;

public final class TimeAxis {

    private long start;
    private long end;

    public TimeAxis(long _start, long _end) {
        set(_start, _end);
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    public Mapper mapper(int x0, int width) {
        return new Mapper(start, end, x0, width);
    }

    void move(int w, double pixelDistance) {
        long diff = (long) ((end - start) / (double) w * pixelDistance);
        move(diff);
    }

    void move(long diff) {
        start += diff;
        end += diff;
        adaptBounds();
    }

    void zoom(int x0, int w, int x, double factor) {
        double multiplier = (end - start) / (double) w * factor;
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
        long now = System.currentTimeMillis(); // assume now < TimeUtils.MAXIMAL_TIME

        start = Math.clamp(start, TimeUtils.MINIMAL_TIME.milli, now);
        end = Math.clamp(end, Math.min(start + TimeUtils.MINUTE_IN_MILLIS, now), now);
        if (end - start < TimeUtils.MINUTE_IN_MILLIS) {
            start = now - TimeUtils.MINUTE_IN_MILLIS;
            end = now;
        }
    }

    public record Mapper(long start, long end, int x0, int width) {
        public int toPixel(long value) {
            return (int) ((double) width / (end - start) * (value - start) + x0);
        }

        public long toValue(int pixel) {
            return (long) (start + (end - start) / (double) width * (pixel - x0));
        }
    }

}
