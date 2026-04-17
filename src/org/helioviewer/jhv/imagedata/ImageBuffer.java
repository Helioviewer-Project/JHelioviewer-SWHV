package org.helioviewer.jhv.imagedata;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.lang.ref.Cleaner;
import java.nio.Buffer;
import java.nio.ByteOrder;

public class ImageBuffer {

    private static final Cleaner cleaner = Cleaner.create();
    private final Cleaner.Cleanable cleanable;
    private final Object cleanerToken = new Object();

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

    public ImageBuffer(int _width, int _height, Format _format, byte[] data) {
        this(_width, _height, _format, data, ImageFilter.Type.None);
    }

    public ImageBuffer(int _width, int _height, Format _format, byte[] data, ImageFilter.Type filterType) {
        width = _width;
        height = _height;
        format = _format;

        Arena arena = Arena.ofShared();
        cleanable = cleaner.register(cleanerToken, new ArenaState(arena));
        buffer = allocate(arena, filter(format, data, _width, _height, filterType));
    }

    public ImageBuffer(int _width, int _height, Format _format, short[] data) {
        this(_width, _height, _format, data, ImageFilter.Type.None);
    }

    public ImageBuffer(int _width, int _height, Format _format, short[] data, ImageFilter.Type filterType) {
        width = _width;
        height = _height;
        format = _format;

        Arena arena = Arena.ofShared();
        cleanable = cleaner.register(cleanerToken, new ArenaState(arena));
        buffer = allocate(arena, filter(format, data, _width, _height, filterType));
    }

    public int byteSize() {
        return width * height * format.bytes;
    }

    private static Buffer allocate(Arena arena, byte[] data) {
        return arena.allocateFrom(ValueLayout.JAVA_BYTE, data).asByteBuffer();
    }

    private static Buffer allocate(Arena arena, short[] data) {
        return arena.allocateFrom(ValueLayout.JAVA_SHORT, data)
                .asByteBuffer()
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
    }

    private static byte[] filter(Format format, byte[] data, int width, int height, ImageFilter.Type filterType) {
        return format == Format.RGBA32 ? data : ImageFilter.filter(data, width, height, filterType);
    }

    private static short[] filter(Format format, short[] data, int width, int height, ImageFilter.Type filterType) {
        return format == Format.RGBA32 ? data : ImageFilter.filter(data, width, height, filterType);
    }

}
