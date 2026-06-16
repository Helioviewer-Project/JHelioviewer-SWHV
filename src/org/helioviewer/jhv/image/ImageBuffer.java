package org.helioviewer.jhv.image;

import java.lang.ref.Cleaner;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.metadata.Region;

import org.lwjgl.system.MemoryUtil;

public final class ImageBuffer {

    private static final Cleaner cleaner = Cleaner.create();

    public enum Format {
        Gray8(1), Gray16F(2), RGBA32(4);

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
    private volatile boolean explicitFreeProtected;

    public static ImageBuffer fromBytes(int width, int height, Format format, byte[] data) {
        return fromBytes(width, height, format, data, ImageFilter.Type.None, null);
    }

    public static ImageBuffer fromBytes(int width, int height, Format format, byte[] data, ImageFilter.Type filterType, @Nullable Region region) {
        if (format == Format.Gray16F)
            throw new IllegalArgumentException("Gray16F image buffers must be created from half-float data");
        byte[] filtered = format == Format.RGBA32 ? data : ImageFilter.filter(data, width, height, filterType, region);
        return new ImageBuffer(width, height, format, allocateFrom(filtered));
    }

    public static ImageBuffer fromShorts(int width, int height, Format format, short[] data, ImageFilter.Type filterType, @Nullable Region region) {
        if (format != Format.Gray16F)
            throw new IllegalArgumentException("Only Gray16F image buffers can be created from half-float data");
        short[] filtered = ImageFilter.filterHalfFloat(data, width, height, filterType, region);
        return new ImageBuffer(width, height, format, allocateFrom(filtered));
    }

    public static WriteBuffer createWriteBuffer(int width, int height, Format format, ImageFilter.Type filterType, @Nullable Region region) {
        return new WriteBuffer(width, height, format, filterType, region);
    }

    private ImageBuffer(int _width, int _height, Format _format, ByteBuffer _buffer) {
        this(_width, _height, _format, _buffer, MemoryUtil.memAddress(_buffer));
    }

    private ImageBuffer(int _width, int _height, Format _format, ShortBuffer _buffer) {
        this(_width, _height, _format, _buffer, MemoryUtil.memAddress(_buffer));
    }

    private ImageBuffer(int _width, int _height, Format _format, Buffer _buffer, long address) {
        width = _width;
        height = _height;
        format = _format;
        buffer = _buffer;
        cleanable = cleaner.register(buffer, new BufferState(address));
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
        private final Region region;
        private final ImageBuffer directBuffer;
        private final byte[] byteArray;
        private final short[] shortArray;
        private final Buffer writeBuffer;

        private WriteBuffer(int _width, int _height, Format _format, ImageFilter.Type _filterType, @Nullable Region _region) {
            width = _width;
            height = _height;
            format = _format;
            filterType = _filterType;
            region = _region;

            if (usesDirectBuffer(format, filterType)) {
                directBuffer = allocate(width, height, format);
                byteArray = null;
                shortArray = null;
                writeBuffer = directBuffer.buffer;
            } else if (format == Format.Gray16F) {
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
                    ? fromShorts(width, height, format, shortArray, filterType, region)
                    : fromBytes(width, height, format, byteArray, filterType, region);
        }

        private static boolean usesDirectBuffer(Format format, ImageFilter.Type filterType) {
            return filterType == ImageFilter.Type.None || format == Format.RGBA32;
        }
    }

    private static final class BufferState implements Runnable {
        private long address;

        private BufferState(long _address) {
            address = _address;
        }

        @Override
        public void run() {
            if (address == 0)
                return;
            MemoryUtil.nmemFree(address);
            address = 0;
        }
    }

    private static ImageBuffer allocate(int width, int height, Format format) {
        int byteSize = byteSize(width, height, format);
        return switch (format) {
            case Gray8, RGBA32 -> new ImageBuffer(width, height, format, MemoryUtil.memAlloc(byteSize));
            case Gray16F -> new ImageBuffer(width, height, format, MemoryUtil.memAllocShort(byteSize / Short.BYTES));
        };
    }

    private static ByteBuffer allocateFrom(byte[] data) {
        ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
        buffer.put(data);
        return buffer.flip();
    }

    private static ShortBuffer allocateFrom(short[] data) {
        ShortBuffer buffer = MemoryUtil.memAllocShort(data.length);
        buffer.put(data);
        return buffer.flip();
    }

    private static int byteSize(int width, int height, Format format) {
        return width * height * format.bytes;
    }

}
