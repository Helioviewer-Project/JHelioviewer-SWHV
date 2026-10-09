package org.helioviewer.jhv.source;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.image.ClipSet;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.image.lut.LUT;

import org.lwjgl.system.MemoryUtil;

// The native client of a local file or JPIP source, and its frames as the reader publishes them to the EDT.
public final class J2KSource implements Source {

    private static final int NO_LEVEL = 10000;

    private final J2KNative client;
    // An entry exists once the frame's header is known.
    private AtomicReferenceArray<ResolutionSet> sets;
    private record Completion(int displayableFrames, int completeFrames) {}

    // Published by the reader; consumers see both prefix lengths from the same update.
    private volatile Completion completion = new Completion(0, 0);

    // JPIP only. The level last requested, NO_LEVEL before the first request; EDT only.
    private J2KReader reader;
    private int currentLevel = NO_LEVEL;
    private volatile boolean downloading;
    private WeakReference<Source.Listener> listener;

    // A local file, or an empty JPIP source for null.
    public J2KSource(@Nullable Path path) throws IOException {
        client = new J2KNative(path);
        if (path == null)
            return;
        try {
            loadFrames();
            for (int i = 0; i < sets.length(); i++)
                update(i);
        } catch (IOException | RuntimeException e) {
            client.close();
            throw e;
        }
    }

    // A JPIP source with its reader connected and the first frame's coarsest level present.
    public static J2KSource open(URI uri) throws IOException {
        J2KSource source = new J2KSource(null);
        try {
            source.reader = new J2KReader(uri, source);
        } catch (IOException | RuntimeException e) {
            source.close();
            throw e;
        }
        return source;
    }

    // Start the reader only after the metadata is parsed and the cache keys are built.
    @Override
    public void start(String[] cacheKey) {
        if (reader != null)
            reader.start(cacheKey);
    }

    // The reader stops first; this may wait, call it off the EDT.
    @Override
    public void close() {
        if (reader != null)
            reader.stop();
        client.close();
    }

    J2KNative client() {
        return client;
    }

    // Acquire the input before allocating output. The caller closes the job after decoding.
    J2KNative.Decode beginDecode(int frame, int level) throws IOException {
        return client.beginDecode(frame, level);
    }

    // Initializes the frame array in the local constructor or JPIP reader constructor,
    // after metadata arrives and before the source is exposed to its view.
    void loadFrames() throws IOException {
        sets = new AtomicReferenceArray<>(client.frames());
    }

    @Override
    public int frames() {
        return sets.length();
    }

    // Publishes the frame's geometry and cumulative native completion.
    // Called during initialization, then only by the JPIP reader.
    void update(int frame) throws IOException {
        J2KNative.Frame info = client.frame(frame);
        int levels = info.width().length;
        if (levels == 0)
            return;

        ResolutionSet set = sets.get(frame);
        if (set == null) {
            ResolutionSet.Level[] resolutions = new ResolutionSet.Level[levels];
            for (int i = 0; i < levels; i++) {
                int width = info.width()[i], height = info.height()[i];
                resolutions[i] = new ResolutionSet.Level(i, width, height);
            }
            set = new ResolutionSet(resolutions, info.channels());
            set.setCompleteLevels(info.ready());
            sets.set(frame, set);
        } else {
            set.setCompleteLevels(info.ready());
        }

        // A complete coarsest level makes the frame displayable.
        Completion previous = completion;
        int displayable = completeUntil(previous.displayableFrames, Integer.MAX_VALUE);
        int complete = completeUntil(previous.completeFrames, 0);
        if (displayable != previous.displayableFrames || complete != previous.completeFrames)
            completion = new Completion(displayable, complete);
    }

    // The first frame at or after from without a complete level.
    private int completeUntil(int from, int level) {
        while (from < sets.length()) {
            ResolutionSet set = sets.get(from);
            if (set == null || !set.getComplete(level))
                break;
            from++;
        }
        return from;
    }

    @Nullable
    @Override
    public String xml(int frame) throws IOException {
        String xml = client.xml(frame);
        return xml == null ? null : xml.trim().replace("&", "&amp;");
    }

    @Nullable
    @Override
    public LUT lut() {
        try {
            ByteBuffer palette = client.palette(0);
            return palette == null ? null : new LUT("built-in", palette);
        } catch (IOException e) {
            Log.warn("Unusable JPEG 2000 palette", e);
            return null;
        }
    }

    @Nullable
    @Override
    public ClipSet clipSet() {
        return null;
    }

    @Override
    public boolean usesFITSParameters() {
        return false;
    }

    // Null while the frame's header is unknown.
    @Nullable
    ResolutionSet geometry(int frame) {
        return sets.get(frame);
    }

    // Requires a known frame header. Use geometry() when it may not have arrived.
    @Override
    public ResolutionSet levels(int frame) {
        return Objects.requireNonNull(sets.get(frame));
    }

    // The level, or the coarsest one the frame has.
    public ResolutionSet.Level level(int frame, int level) {
        return levels(frame).getLevel(level);
    }

    @Override
    public void setListener(@Nullable Source.Listener _listener) {
        listener = _listener == null ? null : new WeakReference<>(_listener);
    }

    // Runs on the reader thread.
    void frameUpdated(int frame) {
        WeakReference<Source.Listener> reference = listener;
        Source.Listener current = reference == null ? null : reference.get();
        if (current != null)
            current.frameUpdated(this, frame);
    }

    void setDownloading(boolean val) {
        downloading = val;
    }

    @Override
    public boolean isDownloading() {
        return downloading;
    }

    // The first request starts the movie download; later ones only for a missing level.
    @Override
    public void request(int frame, int level, boolean priority) {
        if (reader == null)
            return;
        boolean missing = levels(frame).getCompleteLevel(level).level() != level;
        if ((missing || currentLevel == NO_LEVEL) && (priority || level < currentLevel)) {
            reader.signal(new J2KParams.Read(new J2KParams.Decode(frame, level), priority));
            currentLevel = level;
        }
    }

    @Override
    public boolean displayable(int frame) {
        ResolutionSet set = sets.get(frame);
        return set != null && set.isDisplayable();
    }

    // Null: not displayable; false: displayable, incomplete at the level; true: complete at the level.
    @Nullable
    Boolean getFrameStatus(int frame, int level) {
        if (completion.completeFrames == sets.length())
            return true;
        ResolutionSet set = sets.get(frame);
        return set != null && set.isDisplayable() ? set.getComplete(level) : null;
    }

    @Nullable
    @Override
    public Boolean completion(int frame) {
        return getFrameStatus(frame, currentLevel);
    }

    // The last frame of the displayable prefix.
    int getPartialUntil() {
        return Math.max(0, completion.displayableFrames - 1);
    }

    boolean isComplete(int level) {
        return completeUntil(completion.completeFrames, level) == frames();
    }

    @Override
    public boolean isComplete() {
        return isComplete(currentLevel);
    }

    // Gray8 or RGBA rows into a direct buffer; runs on a decode worker.
    // Leaves the output buffer's position unchanged.
    static void decode(J2KNative.Decode job, int x, int y, int width, int height, ByteBuffer pixels) throws IOException {
        String warning = job.run(x, y, width, height, pixels);
        if (warning != null)
            Log.warn(warning);
    }

    @Override
    public ImageBuffer decode(int frame, int level, ImageFilter filter,
                              @Nullable ImageProcessingSettings.FITSParameters fits, @Nullable ClipSet.Range clip) throws IOException {
        try (J2KNative.Decode job = beginDecode(frame, level)) {
            ResolutionSet set = levels(frame);
            ResolutionSet.Level resolution = set.getLevel(level);
            boolean gray = set.numComps == 1;
            try (ImageBuffer.WriteBuffer outBuffer = ImageBuffer.createWriteBuffer(resolution.width(), resolution.height(), gray ? ImageBuffer.Format.Gray8 : ImageBuffer.Format.RGBA32)) {
                decode(job, 0, 0, resolution.width(), resolution.height(), outBuffer.byteBuffer());
                return outBuffer.finish(filter);
            }
        }
    }

    // Gray8 or RGBA rows of a region of a complete level; runs on a decode worker.
    public byte[] decodeRegion(int frame, int level, int x, int y, int width, int height) throws IOException {
        try (J2KNative.Decode job = beginDecode(frame, level)) {
            ByteBuffer direct = MemoryUtil.memAlloc(width * height * levels(frame).numComps);
            try {
                decode(job, x, y, width, height, direct);
                byte[] pixels = new byte[direct.capacity()];
                direct.get(pixels);
                return pixels;
            } finally {
                MemoryUtil.memFree(direct);
            }
        }
    }

}
