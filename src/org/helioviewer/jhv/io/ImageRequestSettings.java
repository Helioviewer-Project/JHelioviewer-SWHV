package org.helioviewer.jhv.io;

import java.util.ArrayList;

import org.helioviewer.jhv.time.TimeUtils;

// The interval and sampling a new image request uses, edited by the movie controls. EDT only.
public final class ImageRequestSettings {

    public sealed interface Sampling permits TimeStep, All, FrameCount {}

    public record TimeStep(int seconds) implements Sampling {
        public TimeStep {
            if (seconds <= 0)
                throw new IllegalArgumentException("Time step must be positive");
        }
    }

    public record All() implements Sampling {}

    public record FrameCount(int frames) implements Sampling {
        public FrameCount {
            if (frames <= 0)
                throw new IllegalArgumentException("Frame count must be positive");
        }
    }

    public interface Listener {
        default void intervalChanged(long start, long end) {}

        default void samplingChanged(Sampling sampling) {}
    }

    private static final ImageRequestSettings instance = new ImageRequestSettings(TimeUtils.START.milli - 2 * TimeUtils.DAY_IN_MILLIS, TimeUtils.START.milli);

    public static ImageRequestSettings instance() {
        return instance;
    }

    private final ArrayList<Listener> listeners = new ArrayList<>();
    private long start;
    private long end;
    private Sampling sampling = new TimeStep(APIRequest.CADENCE_DEFAULT);

    ImageRequestSettings(long _start, long _end) {
        start = accept(_start, TimeUtils.START.milli);
        end = Math.max(start, accept(_end, start));
    }

    // Whole seconds inside the allowed range, else the previous time.
    private static long accept(long time, long previous) {
        return time >= TimeUtils.MINIMAL_TIME.milli && time <= TimeUtils.MAXIMAL_TIME.milli ? TimeUtils.floorSec(time) : previous;
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    public Sampling sampling() {
        return sampling;
    }

    // Normalizes and notifies once when the interval changed.
    public void setInterval(long _start, long _end) {
        long newStart = accept(_start, start);
        long newEnd = Math.max(newStart, accept(_end, end));
        if (newStart == start && newEnd == end)
            return;
        start = newStart;
        end = newEnd;
        listeners.forEach(listener -> listener.intervalChanged(start, end));
    }

    public void setSampling(Sampling _sampling) {
        if (_sampling.equals(sampling))
            return;
        sampling = _sampling;
        listeners.forEach(listener -> listener.samplingChanged(sampling));
    }

    public void addListener(Listener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    // Seconds: the step, every frame, or the interval over the frame count, at least one second.
    public int cadence() {
        return switch (sampling) {
            case TimeStep step -> step.seconds();
            case All ignored -> APIRequest.CADENCE_ALL;
            case FrameCount count -> (int) Math.max(1, Math.round((double) (end - start) / count.frames() / 1000));
        };
    }

    public boolean singleFrame() {
        return sampling instanceof FrameCount(int frames) && frames == 1;
    }

    // A single frame is requested at the start.
    public long requestEnd() {
        return singleFrame() ? start : end;
    }

    public APIRequest request(String server, int sourceId) {
        return new APIRequest(server, sourceId, start, requestEnd(), cadence());
    }

}
