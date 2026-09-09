package org.helioviewer.jhv.image;

import java.lang.ref.Cleaner;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import org.helioviewer.jhv.thread.ParallelRange;

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
        return fromBytes(width, height, format, data, ImageFilter.NONE);
    }

    public static ImageBuffer fromBytes(int width, int height, Format format, byte[] data, ImageFilter filter) {
        if (format == Format.Gray16F)
            throw new IllegalArgumentException("Gray16F image buffers must be created from half-float data");
        if (canUseDirectBuffer(format, filter))
            return new ImageBuffer(width, height, format, allocateFrom(data));
        return fromFloats(width, height, filter.apply(data, width, height));
    }

    public static ImageBuffer fromShorts(int width, int height, Format format, short[] data, ImageFilter filter) {
        if (format != Format.Gray16F)
            throw new IllegalArgumentException("Only Gray16F image buffers can be created from half-float data");
        if (canUseDirectBuffer(format, filter))
            return new ImageBuffer(width, height, format, allocateFrom(data));
        return fromFloats(width, height, filter.apply(data, width, height));
    }

    private static ImageBuffer fromFloats(int width, int height, float[] data) {
        ImageBuffer image = allocate(width, height, Format.Gray16F);
        ShortBuffer buffer = (ShortBuffer) image.buffer;
        ParallelRange.run(height, (from, to) -> {
            for (int y = from; y < to; y++) {
                int rowBase = y * width;
                int rowEnd = rowBase + width;
                for (int idx = rowBase; idx < rowEnd; idx++) {
                    buffer.put(idx, Float.floatToFloat16(Math.clamp(data[idx], 0f, 1f)));
                }
            }
        });
        return image;
    }

    public static WriteBuffer createWriteBuffer(int width, int height, Format format, ImageFilter filter) {
        return new WriteBuffer(width, height, format, filter);
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
        private final Format inputFormat;
        private final ImageFilter filter;
        private final ImageBuffer directBuffer;
        private final Buffer writeBuffer;

        private WriteBuffer(int _width, int _height, Format _format, ImageFilter _filter) {
            width = _width;
            height = _height;
            inputFormat = _format;
            filter = _filter;

            if (ImageBuffer.canUseDirectBuffer(inputFormat, filter)) {
                directBuffer = allocate(width, height, inputFormat);
                writeBuffer = directBuffer.buffer;
            } else if (inputFormat == Format.Gray16F) {
                directBuffer = null;
                writeBuffer = ShortBuffer.allocate(Math.multiplyExact(width, height));
            } else {
                directBuffer = null;
                writeBuffer = ByteBuffer.allocate(byteSize(width, height, inputFormat));
            }
        }

        public ByteBuffer byteBuffer() {
            return (ByteBuffer) writeBuffer;
        }

        public ShortBuffer shortBuffer() {
            return (ShortBuffer) writeBuffer;
        }

        public WriteBuffer clearPixels() {
            if (directBuffer != null)
                MemoryUtil.memSet(MemoryUtil.memAddress(writeBuffer), 0, directBuffer.byteSize());
            else if (writeBuffer instanceof ByteBuffer bytes)
                Arrays.fill(bytes.array(), (byte) 0);
            else
                Arrays.fill(shortBuffer().array(), (short) 0);
            return this;
        }

        public ImageBuffer finish() {
            if (directBuffer != null)
                return directBuffer;
            return writeBuffer instanceof ShortBuffer shorts
                    ? fromShorts(width, height, inputFormat, shorts.array(), filter)
                    : fromBytes(width, height, inputFormat, byteBuffer().array(), filter);
        }

    }

    private static boolean canUseDirectBuffer(Format format, ImageFilter filter) {
        return format == Format.RGBA32 || filter.isNone();
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
        return Math.multiplyExact(Math.multiplyExact(width, height), format.bytes);
    }

}
