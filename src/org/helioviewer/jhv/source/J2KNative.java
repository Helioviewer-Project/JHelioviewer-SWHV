package org.helioviewer.jhv.source;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;

import javax.annotation.Nullable;

// FFM binding of native/jpeg2000/jhv_j2k.h; the library must be loaded first.
// The monitor serializes calls on the source. A decode runs outside it.
final class J2KNative implements AutoCloseable {

    // The client refused a response: the source must not be fed any further.
    @SuppressWarnings("serial")
    static final class Refused extends IOException {
        Refused(String message) {
            super(message);
        }
    }

    // Levels are finest first. Without the frame's header only stream is set.
    // The ready levels are the coarsest ones.
    record Frame(long stream, int channels, int ready, int[] width, int[] height) {}

    record Response(int reason, boolean progressed) {}

    private static final ValueLayout.OfInt I = ValueLayout.JAVA_INT;
    private static final ValueLayout.OfLong L = ValueLayout.JAVA_LONG;
    private static final MemoryLayout P = ValueLayout.ADDRESS;
    private static final int MAX_LEVELS = 33;
    private static final long FRAME_SIZE = 288, FRAME_WIDTH = 20, FRAME_HEIGHT = 152;
    private static final MethodHandle OPEN = bind("open", P, P, P);
    private static final MethodHandle CLOSE = bind("close", null, P);
    private static final MethodHandle NEW_CHANNEL = bind("new_channel", null, P);
    private static final MethodHandle RESPONSE = bind("response", I, P, P, L, I, P, P);
    private static final MethodHandle FRAMES = bind("frames", I, P, P);
    private static final MethodHandle FRAME = bind("frame", I, P, I, P, P);
    private static final MethodHandle XML = bind("xml", L, P, I, P, L, P);
    private static final MethodHandle PALETTE = bind("palette_rgba", I, P, I, P, L, P);
    private static final MethodHandle EXPORT = bind("export", L, P, I, P, L, P);
    private static final MethodHandle IMPORT = bind("import", I, P, I, P, L, P);
    private static final MethodHandle BEGIN = bind("begin_decode", P, P, I, I, P);
    private static final MethodHandle DECODE = bind("decode", L, P, I, I, I, I, P, L, P);
    private static final MethodHandle END = bind("end_decode", null, P);

    private MemorySegment handle;

    // A local JP2/JPX file, or an empty JPIP source for null.
    J2KNative(@Nullable Path path) throws IOException {
        if (path != null && path.toString().indexOf('\0') >= 0)
            throw new IOException("NUL in JPEG 2000 path");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment error = arena.allocate(256);
            MemorySegment encoded = path == null ? MemorySegment.NULL : arena.allocateFrom(path.toString());
            handle = (MemorySegment) call(OPEN, encoded, error);
            if (handle.equals(MemorySegment.NULL))
                throw new IOException(error.getString(0));
        }
    }

    synchronized void newChannel() {
        requireOpen();
        call(NEW_CHANNEL, handle);
    }

    // One response for a frame, or metadata (-1). Each window tracks its own continuation.
    synchronized Response response(ByteBuffer body, int window) throws Refused {
        requireOpen();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment error = arena.allocate(256);
            MemorySegment progress = arena.allocate(I);
            int reason = (int) call(RESPONSE, handle, MemorySegment.ofBuffer(body), (long) body.remaining(), window, progress, error);
            if (reason < 0)
                throw new Refused(error.getString(0));
            return new Response(reason, progress.get(I, 0) != 0);
        }
    }

    synchronized int frames() throws IOException {
        requireOpen();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment error = arena.allocate(256);
            return (int) checked(call(FRAMES, handle, error), error);
        }
    }

    // Fails if the frame cannot be decoded.
    synchronized Frame frame(int frame) throws IOException {
        requireOpen();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment error = arena.allocate(256);
            MemorySegment info = arena.allocate(FRAME_SIZE, 8);
            checked(call(FRAME, handle, frame, info, error), error);
            int levels = info.get(I, 12);
            if (levels < 0 || levels > MAX_LEVELS)
                throw new IOException("Invalid JPEG 2000 level count");
            int[] width = new int[levels], height = new int[levels];
            for (int l = 0; l < levels; l++) {
                width[l] = info.get(I, FRAME_WIDTH + 4L * l);
                height[l] = info.get(I, FRAME_HEIGHT + 4L * l);
            }
            return new Frame(info.get(L, 0), info.get(I, 8), info.get(I, 16), width, height);
        }
    }

    @Nullable
    synchronized String xml(int frame) throws IOException {
        requireOpen();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment error = arena.allocate(256);
            long size = checked(call(XML, handle, frame, MemorySegment.NULL, 0L, error), error);
            if (size == 0)
                return null;
            MemorySegment text = arena.allocate(size);
            checked(call(XML, handle, frame, text, size, error), error);
            return new String(text.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
        }
    }

    @Nullable
    synchronized ByteBuffer palette(int frame) throws IOException {
        requireOpen();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment error = arena.allocate(256);
            int size = (int) checked(call(PALETTE, handle, frame, MemorySegment.NULL, 0L, error), error);
            if (size == 0)
                return null;
            ByteBuffer table = ByteBuffer.allocateDirect(size);
            checked(call(PALETTE, handle, frame, MemorySegment.ofBuffer(table), (long) size, error), error);
            return table;
        }
    }

    // Disk cache entry of a JPIP frame.
    synchronized byte[] exportFrame(int frame) throws IOException {
        requireOpen();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment error = arena.allocate(256);
            long size = checked(call(EXPORT, handle, frame, MemorySegment.NULL, 0L, error), error);
            MemorySegment entry = arena.allocate(size);
            checked(call(EXPORT, handle, frame, entry, size, error), error);
            return entry.toArray(ValueLayout.JAVA_BYTE);
        }
    }

    // A refused entry leaves the source unchanged.
    synchronized void importFrame(int frame, byte[] entry) throws IOException {
        requireOpen();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment error = arena.allocate(256);
            checked(call(IMPORT, handle, frame, arena.allocateFrom(ValueLayout.JAVA_BYTE, entry), (long) entry.length, error), error);
        }
    }

    // Takes an immutable input for a ready level; the job outlives close().
    synchronized Decode beginDecode(int frame, int level) throws IOException {
        requireOpen();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment error = arena.allocate(256);
            MemorySegment job = (MemorySegment) call(BEGIN, handle, frame, level, error);
            if (job.equals(MemorySegment.NULL))
                throw new IOException(error.getString(0));
            return new Decode(job);
        }
    }

    // Use from one thread.
    static final class Decode implements AutoCloseable {

        private MemorySegment job;

        private Decode(MemorySegment _job) {
            job = _job;
        }

        // Fills a direct buffer with Gray8 or RGBA rows of a region of the level
        // without changing its position. Returns Kakadu's warning, if any.
        @Nullable
        String run(int x, int y, int width, int height, ByteBuffer output) throws IOException {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment error = arena.allocate(256);
                checked(call(DECODE, job, x, y, width, height,
                        MemorySegment.ofBuffer(output), (long) output.remaining(), error), error);
                String warning = error.getString(0);
                return warning.isEmpty() ? null : warning;
            }
        }

        @Override
        public void close() {
            if (!job.equals(MemorySegment.NULL)) {
                call(END, job);
                job = MemorySegment.NULL;
            }
        }

    }

    @Override
    public synchronized void close() {
        if (!handle.equals(MemorySegment.NULL)) {
            call(CLOSE, handle);
            handle = MemorySegment.NULL;
        }
    }

    private void requireOpen() {
        if (handle.equals(MemorySegment.NULL))
            throw new CancellationException("JPEG 2000 source is closed");
    }

    @SuppressWarnings("restricted")
    private static MethodHandle bind(String name, @Nullable MemoryLayout result, MemoryLayout... arguments) {
        FunctionDescriptor descriptor = result == null ? FunctionDescriptor.ofVoid(arguments) : FunctionDescriptor.of(result, arguments);
        return Linker.nativeLinker().downcallHandle(SymbolLookup.loaderLookup().find("jhv_j2k_" + name)
                .orElseThrow(() -> new UnsatisfiedLinkError("Missing jhv_j2k_" + name)), descriptor);
    }

    private static Object call(MethodHandle method, Object... arguments) {
        try {
            return method.invokeWithArguments(arguments);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError("Invalid JPEG 2000 native binding", e);
        }
    }

    private static long checked(Object result, MemorySegment error) throws IOException {
        long value = ((Number) result).longValue();
        if (value < 0)
            throw new IOException(error.getString(0));
        return value;
    }

}
