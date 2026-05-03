package org.helioviewer.jhv.imagedata;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.lang.ref.Cleaner;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public final class ImageBuffer {

    private static final Cleaner cleaner = Cleaner.create();

    public enum Format {
        Gray8(1), Gray16(2), RGBA32(4);

        public final int bytes;

        Format(int _bytes) {
            bytes = _bytes;
        }
    }

    public final int width;
    public final int height;
    public final Format format;
    public final Buffer buffer;

    private final Cleaner.Cleanable cleanable;
    @SuppressWarnings("FieldCanBeLocal")
    // Cleaner tracks reachability of this token; the cleanup action must not capture this ImageBuffer.
    private final Object cleanerToken = new Object();
    private volatile boolean explicitFreeProtected;

    public static ImageBuffer fromBytes(int width, int height, Format format, byte[] data) {
        return fromBytes(width, height, format, data, ImageFilter.Type.None);
    }

    public static ImageBuffer fromBytes(int width, int height, Format format, byte[] data, ImageFilter.Type filterType) {
        if (format == Format.Gray16)
            throw new IllegalArgumentException("Gray16 image buffers must be created from short data");
        byte[] filtered = format == Format.RGBA32 ? data : ImageFilter.filter(data, width, height, filterType);
        Arena arena = Arena.ofShared();
        return new ImageBuffer(width, height, format, arena, allocateFrom(arena, filtered));
    }

    public static ImageBuffer fromShorts(int width, int height, Format format, short[] data, ImageFilter.Type filterType) {
        if (format != Format.Gray16)
            throw new IllegalArgumentException("Only Gray16 image buffers can be created from short data");
        short[] filtered = ImageFilter.filter(data, width, height, filterType);
        Arena arena = Arena.ofShared();
        return new ImageBuffer(width, height, format, arena, allocateFrom(arena, filtered));
    }

    public static WriteBuffer createWriteBuffer(int width, int height, Format format, ImageFilter.Type filterType) {
        return new WriteBuffer(width, height, format, filterType);
    }

    private ImageBuffer(int _width, int _height, Format _format, Arena arena, Buffer _buffer) {
        width = _width;
        height = _height;
        format = _format;
        buffer = _buffer;

        cleanable = cleaner.register(cleanerToken, new ArenaState(arena));
    }

    public int byteSize() {
        return byteSize(width, height, format);
    }

    public void protectFromExplicitFree() {
        explicitFreeProtected = true;
    }

    public void allowExplicitFree() {
        explicitFreeProtected = false;
    }

    boolean free() {
        if (explicitFreeProtected)
            return false;
        cleanable.clean();
        return true;
    }

    public static final class WriteBuffer {
        private final int width;
        private final int height;
        private final Format format;
        private final ImageFilter.Type filterType;
        private final ImageBuffer directBuffer;
        private final byte[] byteArray;
        private final short[] shortArray;
        private final Buffer writeBuffer;

        private WriteBuffer(int _width, int _height, Format _format, ImageFilter.Type _filterType) {
            width = _width;
            height = _height;
            format = _format;
            filterType = _filterType;

            if (usesDirectBuffer(format, filterType)) {
                Arena arena = Arena.ofShared();
                directBuffer = new ImageBuffer(width, height, format, arena, allocate(arena, byteSize(width, height, format), format));
                byteArray = null;
                shortArray = null;
                writeBuffer = directBuffer.buffer;
            } else if (format == Format.Gray16) {
                directBuffer = null;
                byteArray = null;
                shortArray = new short[width * height];
                writeBuffer = ShortBuffer.wrap(shortArray);
            } else {
                directBuffer = null;
                byteArray = new byte[byteSize(width, height, format)];
                shortArray = null;
                writeBuffer = ByteBuffer.wrap(byteArray);
            }
        }

        public ByteBuffer byteBuffer() {
            return (ByteBuffer) writeBuffer;
        }

        public ShortBuffer shortBuffer() {
            return (ShortBuffer) writeBuffer;
        }

        public ImageBuffer finish() {
            if (directBuffer != null)
                return directBuffer;
            return shortArray != null
                    ? fromShorts(width, height, format, shortArray, filterType)
                    : fromBytes(width, height, format, byteArray, filterType);
        }

        private static boolean usesDirectBuffer(Format format, ImageFilter.Type filterType) {
            return filterType == ImageFilter.Type.None || format == Format.RGBA32;
        }
    }

    private static final class ArenaState implements Runnable {
        private Arena arena;

        private ArenaState(Arena _arena) {
            arena = _arena;
        }

        @Override
        public void run() {
            if (arena == null)
                return;
            arena.close();
            arena = null;
        }
    }

    private static Buffer allocate(Arena arena, int byteSize, Format format) {
        ByteBuffer byteBuffer = arena.allocate(byteSize).asByteBuffer();
        return format == Format.Gray16 ? byteBuffer.order(ByteOrder.nativeOrder()).asShortBuffer() : byteBuffer;
    }

    private static Buffer allocateFrom(Arena arena, byte[] data) {
        return arena.allocateFrom(ValueLayout.JAVA_BYTE, data).asByteBuffer();
    }

    private static Buffer allocateFrom(Arena arena, short[] data) {
        return arena.allocateFrom(ValueLayout.JAVA_SHORT, data)
                .asByteBuffer()
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
    }

    private static int byteSize(int width, int height, Format format) {
        return width * height * format.bytes;
    }

}
