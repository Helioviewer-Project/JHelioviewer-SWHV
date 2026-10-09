package org.helioviewer.jhv.layers;

import java.awt.EventQueue;
import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.image.ClipSet;
import org.helioviewer.jhv.image.ImageBufferCache;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.metadata.BasicMetaData;
import org.helioviewer.jhv.metadata.FitsMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.NullMetaData;
import org.helioviewer.jhv.source.Source;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.time.JHVTime;

// The timeline of an image layer: frames sorted by time, each from a source or empty. EDT only.
public final class Frames implements Source.Listener {

    public record Frame(JHVTime time, MetaData metaData, String xml, @Nullable Source source, int index) {}

    public interface Listener {
        // On the EDT, after the source reported its frame ready.
        void frameReady(Frames frames, Source source, int frame);
    }

    public static final String EMPTY_METAXML = "<xml/>";

    private static final AtomicInteger serials = new AtomicInteger();
    private static final Cleaner reaper = Cleaner.create();

    private final int serial = serials.incrementAndGet();
    private final Frame[] frames;
    private final JHVTime[] times;
    private final List<Source> sources;
    private final boolean hasFITS;
    private final @Nullable ClipSet clipSet;
    private final Cleaner.Cleanable closer;

    private @Nullable Listener listener;
    private int current;
    private JHVTime requested;

    // Empty frames at a cadence, the last one at end.
    public static Frames synthetic(long start, long end, int cadence) {
        if (end < start)
            throw new IllegalArgumentException("End cannot be earlier than start");

        List<Frame> list = new ArrayList<>();
        list.add(empty(start));
        if (cadence > 0 && end > start) {
            long t = start;
            while (true) {
                t += cadence * 1000L;
                if (t >= end) {
                    list.add(empty(end));
                    break;
                } else
                    list.add(empty(t));
            }
        }
        return new Frames(list);
    }

    private static Frame empty(long milli) {
        JHVTime time = new JHVTime(milli);
        return new Frame(time, new NullMetaData(time), EMPTY_METAXML, null, 0);
    }

    // One frame without pixels, for a layer that has not loaded yet.
    public static Frames placeholder() {
        return new Frames(List.of(new Frame(BasicMetaData.EMPTY.getViewpoint().time, BasicMetaData.EMPTY, EMPTY_METAXML, null, 0)));
    }

    // Times strictly increasing; the loader sorts and validates them.
    public Frames(List<Frame> _frames) {
        if (_frames.isEmpty())
            throw new IllegalArgumentException("No frames");
        frames = _frames.toArray(Frame[]::new);
        times = new JHVTime[frames.length];

        LinkedHashSet<Source> distinct = new LinkedHashSet<>();
        List<ClipSet> clipSets = new ArrayList<>();
        boolean fits = false;
        for (int i = 0; i < frames.length; i++) {
            times[i] = frames[i].time;
            if (i > 0 && times[i].milli <= times[i - 1].milli)
                throw new IllegalArgumentException("Frames out of order");
            Source source = frames[i].source;
            if (source != null) {
                distinct.add(source);
                fits |= source.usesFITSParameters();
                clipSets.add(source.clipSet());
            }
        }
        sources = List.copyOf(distinct);
        hasFITS = fits;
        clipSet = ClipSet.median(clipSets);
        requested = times[0];

        for (Source source : sources)
            source.setListener(this);
        closer = reaper.register(this, new Closer(serial, sources));
    }

    // Holds no reference to the timeline; sources hold their listener weakly.
    private record Closer(int serial, List<Source> sources) implements Runnable {
        @Override
        public void run() {
            ImageBufferCache.invalidateOwners(Set.of(serial));
            if (sources.isEmpty())
                return;
            // Stopping a reader may wait.
            AppThread.create(() -> {
                for (Source source : sources) {
                    try {
                        source.close();
                    } catch (Exception e) {
                        Log.error(e);
                    }
                }
            }, "JHV-FramesCloser").start();
        }
    }

    // Retires the cached images and closes the sources on a thread.
    public void close() {
        closer.clean();
    }

    public void clearCache() {
        ImageBufferCache.invalidateOwners(Set.of(serial));
    }

    // The cache owner of this timeline's decoded images.
    int serial() {
        return serial;
    }

    public void setListener(@Nullable Listener _listener) {
        listener = _listener;
    }

    @Override
    public void frameUpdated(Source source, int frame) {
        EventQueue.invokeLater(() -> {
            if (listener != null)
                listener.frameReady(this, source, frame);
        });
    }

    public int size() {
        return frames.length;
    }

    public Frame get(int i) {
        return frames[clamp(i)];
    }

    public JHVTime time(int i) {
        return times[clamp(i)];
    }

    private int clamp(int i) {
        return Math.clamp(i, 0, frames.length - 1);
    }

    public JHVTime first() {
        return times[0];
    }

    public JHVTime last() {
        return times[frames.length - 1];
    }

    private int nearestIndex(JHVTime time) {
        int idx = Arrays.binarySearch(times, time);
        if (idx >= 0)
            return idx;

        int ip = -idx - 1;
        if (ip == 0)
            return 0;
        if (ip >= frames.length)
            return frames.length - 1;

        JHVTime f = times[ip - 1];
        JHVTime c = times[ip];
        return time.milli - f.milli < c.milli - time.milli ? ip - 1 : ip;
    }

    public JHVTime nearest(JHVTime time) {
        return times[nearestIndex(time)];
    }

    // The latest time before this one, or the first.
    public JHVTime lower(JHVTime time) {
        int idx = Arrays.binarySearch(times, time);
        int i = (idx >= 0 ? idx : -idx - 1) - 1;
        return times[Math.max(i, 0)];
    }

    // The earliest time after this one, or the last.
    public JHVTime higher(JHVTime time) {
        int idx = Arrays.binarySearch(times, time);
        int i = idx >= 0 ? idx + 1 : -idx - 1;
        return times[Math.min(i, frames.length - 1)];
    }

    public MetaData metaData(JHVTime time) {
        return frames[nearestIndex(time)].metaData;
    }

    public String xml(JHVTime time) {
        return frames[nearestIndex(time)].xml;
    }

    public int current() {
        return current;
    }

    public JHVTime requested() {
        return requested;
    }

    private boolean displayable(int i) {
        Frame frame = frames[i];
        return frame.source == null || frame.source.displayable(frame.index);
    }

    // Selects the nearest frame, or the nearest displayable one in time while that one is not.
    // True when the selection moved.
    public boolean select(JHVTime time) {
        requested = time;
        int i = nearestIndex(time);
        if (!displayable(i)) {
            int lo = i - 1, hi = i + 1;
            while (lo >= 0 && !displayable(lo))
                lo--;
            while (hi < frames.length && !displayable(hi))
                hi++;
            if (lo < 0 && hi >= frames.length)
                return false;
            if (lo < 0)
                i = hi;
            else if (hi >= frames.length)
                i = lo;
            else
                i = time.milli - times[lo].milli <= times[hi].milli - time.milli ? lo : hi;
        }
        if (i == current)
            return false;
        current = i;
        return true;
    }

    public boolean hasFITS() {
        return hasFITS;
    }

    @Nullable
    public ClipSet clipSet() {
        return clipSet;
    }

    @Nullable
    public LUT defaultLUT() {
        Frame first = frames[0];
        LUT lut = first.source == null ? null : first.source.lut();
        if (lut != null)
            return lut;
        return first.metaData instanceof FitsMetaData fm ? LUT.get(fm) : null;
    }

    // Snapshot: null when unavailable, false when partial, true when complete.
    @Nullable
    public Boolean completion(int i) {
        Frame frame = frames[clamp(i)];
        return frame.source == null ? Boolean.TRUE : frame.source.completion(frame.index);
    }

    public boolean isComplete() {
        for (Source source : sources) {
            if (!source.isComplete())
                return false;
        }
        return true;
    }

    public boolean isDownloading() {
        for (Source source : sources) {
            if (source.isDownloading())
                return true;
        }
        return false;
    }

}
