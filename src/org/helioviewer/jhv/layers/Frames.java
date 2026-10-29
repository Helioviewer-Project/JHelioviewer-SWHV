package org.helioviewer.jhv.layers;

import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.helioviewer.jhv.thread.UiThread;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;
import org.helioviewer.jhv.wcs.ImageGeometry;
import org.helioviewer.jhv.wcs.ImageGrid;

// The timeline of an image layer: frames sorted by time, each from a source or empty. EDT only.
public final class Frames extends TimeMap<Frames.Frame> implements Source.Listener {

    public record Frame(JHVTime time, MetaData metaData, String xml, @Nullable Source source, int index) {
        // The frame's footprint is independent of the selected decode level.
        ImageGeometry geometry() {
            ImageGeometry declared = metaData.getGeometry();
            if (source == null)
                return declared;
            ImageGrid full = source.levels(index).getLevel(0);
            return declared.withSize(full.width(), full.height());
        }
    }

    public interface Listener {
        // On the EDT, after the source reported its frame ready.
        void frameReady(Frames frames, Source source, int frame);
    }

    public static final String EMPTY_METAXML = "<xml/>";

    private static final AtomicInteger serials = new AtomicInteger();
    private static final Cleaner reaper = Cleaner.create();

    private final int serial = serials.incrementAndGet();
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
        super(_frames, Frame::time);

        LinkedHashSet<Source> distinct = new LinkedHashSet<>();
        List<ClipSet> clipSets = new ArrayList<>();
        boolean fits = false;
        for (int i = 0; i < size(); i++) {
            Source source = indexedValue(i).source;
            if (source != null) {
                distinct.add(source);
                fits |= source.usesFITSParameters();
                clipSets.add(source.clipSet());
            }
        }
        sources = List.copyOf(distinct);
        hasFITS = fits;
        clipSet = ClipSet.median(clipSets);
        requested = firstKey();

        for (Source source : sources)
            source.setListener(this);
        closer = reaper.register(this, new Closer(serial, sources));
    }

    // Holds no reference to the timeline; sources hold their listener weakly.
    private record Closer(int serial, List<Source> sources) implements Runnable {
        @Override
        public void run() {
            ImageBufferCache.invalidateOwner(serial);
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
        ImageBufferCache.invalidateOwner(serial);
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
        UiThread.post(() -> {
            if (listener != null)
                listener.frameReady(this, source, frame);
        });
    }

    public Frame get(int i) {
        return indexedValue(i);
    }

    public JHVTime time(int i) {
        return key(i);
    }

    public JHVTime first() {
        return firstKey();
    }

    public JHVTime last() {
        return lastKey();
    }

    public JHVTime nearest(JHVTime time) {
        return nearestKey(time);
    }

    public JHVTime lower(JHVTime time) {
        return lowerKey(time);
    }

    public JHVTime higher(JHVTime time) {
        return higherKey(time);
    }

    public MetaData metaData(JHVTime time) {
        return nearestValue(time).metaData;
    }

    public int current() {
        return current;
    }

    public JHVTime requested() {
        return requested;
    }

    // A remote frame whose coarsest level has not arrived.
    private boolean missing(int i) {
        Frame frame = indexedValue(i);
        return frame.source != null && !frame.source.displayable(frame.index);
    }

    // Selects the nearest frame, or the nearest displayable one in time while that one is not.
    // True when the selection moved.
    public boolean select(JHVTime time) {
        requested = time;
        int i = nearestIndex(time);
        if (missing(i)) {
            int count = size();
            int lo = i - 1, hi = i + 1;
            while (lo >= 0 && missing(lo))
                lo--;
            while (hi < count && missing(hi))
                hi++;
            if (lo < 0 && hi >= count)
                return false;
            if (lo < 0)
                i = hi;
            else if (hi >= count)
                i = lo;
            else
                i = time.milli - key(lo).milli <= key(hi).milli - time.milli ? lo : hi;
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
        Frame first = indexedValue(0);
        LUT lut = first.source == null ? null : first.source.lut();
        if (lut != null)
            return lut;
        return first.metaData instanceof FitsMetaData fm ? LUT.get(fm) : null;
    }

    // Snapshot: null when unavailable, false when partial, true when complete.
    @Nullable
    public Boolean completion(int i) {
        Frame frame = indexedValue(i);
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
