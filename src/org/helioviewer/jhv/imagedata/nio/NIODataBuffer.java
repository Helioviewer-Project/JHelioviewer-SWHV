package org.helioviewer.jhv.imagedata.nio;

import java.awt.image.DataBuffer;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

abstract class NIODataBuffer extends DataBuffer {

    private Buffer buffer;
    private Arena arena;

    private NIODataBuffer(int type, int size, int numBanks) {
        super(type, size, numBanks);

        int componentSize = DataBuffer.getDataTypeSize(type) / 8;
        long length = ((long) size) * componentSize * numBanks;

        arena = Arena.ofShared();
        MemorySegment segment = arena.allocate(length, componentSize);
        ByteBuffer byteBuffer = segment.asByteBuffer().order(ByteOrder.nativeOrder());
        switch (type) {
            case DataBuffer.TYPE_BYTE -> buffer = byteBuffer;
            case DataBuffer.TYPE_USHORT -> buffer = byteBuffer.asShortBuffer();
            case DataBuffer.TYPE_INT -> buffer = byteBuffer.asIntBuffer();
            default -> throw new IllegalArgumentException("Unsupported data type: " + type);
        }
    }

    Buffer getBuffer() {
        return buffer;
    }

    void free() {
        if (arena == null)
            return;

        arena.close();
        arena = null;
        buffer = null;
    }

    @Override
    public String toString() {
        return String.format("NIODataBuffer: %s", buffer);
    }

    public static DataBuffer create(int type, int size, int numBanks) {
        return switch (type) {
            case DataBuffer.TYPE_BYTE -> new DataBufferByte(size, numBanks);
            case DataBuffer.TYPE_USHORT -> new DataBufferUShort(size, numBanks);
            case DataBuffer.TYPE_INT -> new DataBufferInt(size, numBanks);
            default -> throw new IllegalArgumentException("Unsupported data type: " + type);
        };
    }

    static class DataBufferByte extends NIODataBuffer {
        private final ByteBuffer buffer;

        DataBufferByte(int size, int numBanks) {
            super(DataBuffer.TYPE_BYTE, size, numBanks);
            buffer = (ByteBuffer) super.buffer;
        }

        @Override
        public int getElem(int bank, int i) {
            return buffer.get(bank * size + i) & 0xff;
        }

        @Override
        public void setElem(int bank, int i, int val) {
            buffer.put(bank * size + i, (byte) val);
        }
    }

    static class DataBufferUShort extends NIODataBuffer {
        private final ShortBuffer buffer;

        DataBufferUShort(int size, int numBanks) {
            super(DataBuffer.TYPE_USHORT, size, numBanks);
            buffer = (ShortBuffer) super.buffer;
        }

        @Override
        public int getElem(int bank, int i) {
            return buffer.get(bank * size + i) & 0xffff;
        }

        @Override
        public void setElem(int bank, int i, int val) {
            buffer.put(bank * size + i, (short) val);
        }
    }

    static class DataBufferInt extends NIODataBuffer {
        private final IntBuffer buffer;

        DataBufferInt(int size, int numBanks) {
            super(DataBuffer.TYPE_INT, size, numBanks);
            buffer = (IntBuffer) super.buffer;
        }

        @Override
        public int getElem(int bank, int i) {
            return buffer.get(bank * size + i);
        }

        @Override
        public void setElem(int bank, int i, int val) {
            buffer.put(bank * size + i, val);
        }
    }

}
