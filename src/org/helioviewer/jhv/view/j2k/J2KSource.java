package org.helioviewer.jhv.view.j2k;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.image.lut.LUT;

import org.lwjgl.system.MemoryUtil;

// The native client of a local file or JPIP source, and its frames as the reader publishes them to the EDT.
public final class J2KSource {

    private final J2KNative client;
    // An entry exists once the frame's header is known.
    private AtomicReferenceArray<ResolutionSet> sets;

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

    public void close() {
        client.close();
    }

    J2KNative client() {
        return client;
    }

    // JPIP: once the metadata has arrived.
    void loadFrames() throws IOException {
        sets = new AtomicReferenceArray<>(client.frames());
    }

    int frames() {
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
            sets.set(frame, set);
        }
        set.setCompleteLevels(info.ready());
    }

    @Nullable
    public String xml(int frame) throws IOException {
        String xml = client.xml(frame);
        return xml == null ? null : xml.trim().replace("&", "&amp;");
    }

    @Nullable
    LUT lut() {
        J2KNative.Palette palette;
        try {
            palette = client.palette(0);
        } catch (IOException e) {
            Log.warn("Unusable JPEG 2000 palette", e);
            return null;
        }
        if (palette == null)
            return null;

        int entries = palette.entries(), channels = palette.channels();
        byte[] values = palette.values();
        byte[] red = new byte[entries], green = new byte[entries], blue = new byte[entries];
        for (int i = 0; i < entries; i++) {
            red[i] = values[i * channels];
            green[i] = values[i * channels + (channels < 3 ? 0 : 1)];
            blue[i] = values[i * channels + (channels < 3 ? 0 : 2)];
        }
        return LUT.fromOpaqueRgb("built-in", red, green, blue);
    }

    // Null while the frame's header is unknown.
    @Nullable
    ResolutionSet geometry(int frame) {
        return sets.get(frame);
    }

    // Requires a known frame header. Use geometry() when it may not have arrived.
    ResolutionSet resolutionSet(int frame) {
        return Objects.requireNonNull(sets.get(frame));
    }

    // The level, or the coarsest one the frame has.
    public ResolutionSet.Level level(int frame, int level) {
        return resolutionSet(frame).getLevel(level);
    }

    // Null: not displayable; false: displayable, incomplete at the level; true: complete at the level.
    @Nullable
    Boolean getFrameStatus(int frame, int level) {
        ResolutionSet set = sets.get(frame);
        return set != null && set.isDisplayable() ? set.getComplete(level) : null;
    }

    // The last frame of the displayable prefix.
    int getPartialUntil() {
        int i = 0;
        while (i < sets.length() && getFrameStatus(i, 0) != null)
            i++;
        return Math.max(0, i - 1);
    }

    boolean isComplete(int level) {
        for (int i = 0; i < sets.length(); i++) {
            ResolutionSet set = sets.get(i);
            if (set == null || !set.getComplete(level))
                return false;
        }
        return true;
    }

    // Acquire the input before allocating pixels. The job outlives close().
    J2KNative.Decode beginDecode(int frame, int level) throws IOException {
        return client.beginDecode(frame, level);
    }

    // Gray8 or RGBA rows into a direct buffer; runs on a decode worker.
    // Leaves the output buffer's position unchanged.
    static void decode(J2KNative.Decode job, int x, int y, int width, int height, ByteBuffer pixels) throws IOException {
        String warning = job.run(x, y, width, height, pixels);
        if (warning != null)
            Log.warn(warning);
    }

    // Gray8 or RGBA rows of a region of a complete level; runs on a decode worker.
    public byte[] decode(int frame, int level, int x, int y, int width, int height) throws IOException {
        try (J2KNative.Decode job = beginDecode(frame, level)) {
            ByteBuffer direct = MemoryUtil.memAlloc(width * height * resolutionSet(frame).numComps);
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
