package org.helioviewer.jhv.view.j2k;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;

import org.lwjgl.system.MemoryUtil;

// The native client of a local file or JPIP source, and its frames as the reader publishes them to the EDT.
final class J2KSource {

    private final J2KNative client;
    // An entry exists once the frame's header is known.
    private AtomicReferenceArray<ResolutionSet> sets;

    // A local file, or an empty JPIP source for null.
    J2KSource(@Nullable Path path) throws IOException {
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

    void close() {
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

    // After new data for a frame: its header, if now known, and its complete levels.
    void update(int frame) throws IOException {
        J2KNative.Frame info = client.frame(frame);
        int levels = info.width().length;
        if (levels == 0)
            return;

        ResolutionSet set = sets.get(frame);
        if (set == null) {
            ResolutionSet.Level[] resolutions = new ResolutionSet.Level[levels];
            int width0 = info.width()[0], height0 = info.height()[0];
            for (int i = 0; i < levels; i++) {
                int width = info.width()[i], height = info.height()[i];
                resolutions[i] = new ResolutionSet.Level(i, width, height, width0 / (double) width, height0 / (double) height);
            }
            set = new ResolutionSet(resolutions, info.channels());
            sets.set(frame, set);
        }
        if (info.ready() > 0)
            set.setComplete(levels - info.ready());
    }

    @Nullable
    String xml(int frame) throws IOException {
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

    // For displayable frames.
    ResolutionSet resolutionSet(int frame) {
        ResolutionSet set = sets.get(frame);
        if (set == null) {
            Log.error("resolutionSet[" + frame + "] is null"); // never happened?
            return sets.get(0);
        }
        return set;
    }

    // Null: not displayable; false: displayable, incomplete at the level; true: complete at the level.
    @Nullable
    AtomicBoolean getFrameStatus(int frame, int level) {
        ResolutionSet set = sets.get(frame);
        return set == null || !set.isDisplayable() ? null : set.getComplete(level);
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
            if (set == null || !set.getComplete(level).get())
                return false;
        }
        return true;
    }

    // A region of a complete level; runs on a decode worker.
    DecodedImage decode(J2KParams.Decode params, ImageFilter.Type filterType, MetaData metaData, double factorX, double factorY) throws IOException {
        J2KParams.SubImage roi = params.subImage;
        boolean gray = resolutionSet(params.frame).numComps == 1;
        try (J2KNative.Decode job = client.beginDecode(params.frame, params.level)) {
            Region imageRegion = metaData.roiToRegion(roi.x(), roi.y(), roi.w(), roi.h(), factorX, factorY);
            ImageFilter filter = ImageFilter.of(filterType, imageRegion, metaData);
            ImageBuffer.WriteBuffer outBuffer = ImageBuffer.createWriteBuffer(roi.w(), roi.h(), gray ? ImageBuffer.Format.Gray8 : ImageBuffer.Format.RGBA32, filter);
            ByteBuffer pixels = outBuffer.byteBuffer();

            String warning;
            if (pixels.isDirect()) {
                warning = job.run(roi.x(), roi.y(), roi.w(), roi.h(), pixels.duplicate());
            } else { // a filtered gray image is built on the heap
                ByteBuffer direct = MemoryUtil.memAlloc(pixels.capacity());
                try {
                    warning = job.run(roi.x(), roi.y(), roi.w(), roi.h(), direct);
                    pixels.put(0, direct, 0, pixels.capacity());
                } finally {
                    MemoryUtil.memFree(direct);
                }
            }
            if (warning != null)
                Log.warn(warning);
            return new DecodedImage(outBuffer.finish(), imageRegion);
        }
    }

}
