package org.helioviewer.jhv.image;

import java.lang.ref.Cleaner;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

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

    public static ImageBuffer fromBytes(int width, int height, Format format, byte[] data) {
        return fromBytes(width, height, format, data, ImageFilter.NONE);
    }

    public static ImageBuffer fromBytes(int width, int height, Format format, byte[] data, ImageFilter filter) {
        if (format == Format.Gray16F)
            throw new IllegalArgumentException("Gray16F image buffers must be created from half-float data");
        if (keepsOriginalPixels(format, filter))
            return new ImageBuffer(width, height, format, allocateFrom(data));
        return fromFloats(width, height, filter.apply(ByteBuffer.wrap(data), width, height));
    }

    public static ImageBuffer fromShorts(int width, int height, short[] data, ImageFilter filter) {
        if (filter.isNone())
            return new ImageBuffer(width, height, Format.Gray16F, allocateFrom(data));
        return fromFloats(width, height, filter.apply(ShortBuffer.wrap(data), width, height));
    }

    private static ImageBuffer fromFloats(int width, int height, float[] data) {
        try (WriteBuffer output = createWriteBuffer(width, height, Format.Gray16F)) {
            ShortBuffer buffer = output.shortBuffer();
            ParallelRange.run(height, (from, to) -> {
                for (int y = from; y < to; y++) {
                    int rowBase = y * width;
                    int rowEnd = rowBase + width;
                    for (int idx = rowBase; idx < rowEnd; idx++) {
                        buffer.put(idx, Float.floatToFloat16(Math.clamp(data[idx], 0f, 1f)));
                    }
                }
            });
            return output.finish();
        }
    }

    public static WriteBuffer createWriteBuffer(int width, int height, Format format) {
        return new WriteBuffer(width, height, format);
    }

    private ImageBuffer(int _width, int _height, Format _format, Buffer _buffer) {
        width = _width;
        height = _height;
        format = _format;
        buffer = _buffer;
        cleanable = cleaner.register(buffer, new BufferState(MemoryUtil.memAddress(buffer)));
    }

    public int byteSize() {
        return byteSize(width, height, format);
    }

    void free() {
        cleanable.clean();
    }

    public static final class WriteBuffer implements AutoCloseable {
        private ImageBuffer image;

        private WriteBuffer(int width, int height, Format format) {
            image = allocate(width, height, format);
        }

        public ByteBuffer byteBuffer() {
            return (ByteBuffer) image.buffer;
        }

        public ShortBuffer shortBuffer() {
            return (ShortBuffer) image.buffer;
        }

        public WriteBuffer clearPixels() {
            image.buffer.clear();
            MemoryUtil.memSet(MemoryUtil.memAddress(image.buffer), 0, image.byteSize());
            return this;
        }

        // Transfers the written buffer to the returned image.
        public ImageBuffer finish() {
            if (image == null)
                throw new IllegalStateException("Image writer is closed");
            ImageBuffer output = image;
            image = null;
            output.buffer.clear();
            return output;
        }

        // Filtering consumes the input; the returned image owns the final pixels.
        public ImageBuffer finish(ImageFilter filter) {
            ImageBuffer input = finish();
            if (keepsOriginalPixels(input.format, filter))
                return input;

            float[] filtered;
            try {
                filtered = input.buffer instanceof ShortBuffer shorts
                        ? filter.apply(shorts, input.width, input.height)
                        : filter.apply((ByteBuffer) input.buffer, input.width, input.height);
            } finally {
                input.free();
            }
            return fromFloats(input.width, input.height, filtered);
        }

        @Override
        public void close() {
            if (image != null) {
                image.free();
                image = null;
            }
        }

    }

    private static boolean keepsOriginalPixels(Format format, ImageFilter filter) {
        return format == Format.RGBA32 || filter.isNone();
    }

    private static final class BufferState implements Runnable {
        private final long address;

        private BufferState(long _address) {
            address = _address;
        }

        @Override
        public void run() {
            MemoryUtil.nmemFree(address);
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
