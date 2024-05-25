package org.helioviewer.jhv.imagedata.nio;

import java.awt.image.DataBuffer;
import java.nio.*;

import org.lwjgl.system.MemoryUtil;

abstract class NIODataBuffer extends DataBuffer {

    private Buffer buffer;

    private NIODataBuffer(int type, int size, int numBanks) {
        super(type, size, numBanks);

        int componentSize = DataBuffer.getDataTypeSize(type) / 8;
        long length = ((long) size) * componentSize * numBanks;

        ByteBuffer byteBuffer = MemoryUtil.memAlloc((int) length).order(ByteOrder.nativeOrder());
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
        MemoryUtil.memFree(buffer);
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

    public static DataBuffer create(Buffer buffer) {
        if (buffer instanceof ByteBuffer) {
            ByteDataBuffer ret = new ByteDataBuffer();
            ret.setBuffer((ByteBuffer) buffer);
            return ret;
        } else if (buffer instanceof ShortBuffer) {
            UShortDataBuffer ret = new UShortDataBuffer();
            ret.setBuffer((ShortBuffer) buffer);
            return ret;
        } else if (buffer instanceof IntBuffer) {
            IntDataBuffer ret = new IntDataBuffer();
            ret.setBuffer((IntBuffer) buffer);
            return ret;
        } else
            throw new IllegalArgumentException("Unsupported data type: " + buffer);
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

    static class ByteDataBuffer extends DataBuffer {
        private ByteBuffer buffer;

        ByteDataBuffer() {
            super(DataBuffer.TYPE_BYTE, 0, 0);
        }

        void setBuffer(ByteBuffer _buffer) {
            buffer = _buffer;
        }

        @Override
        public int getElem(int bank, int i) {
            return buffer.get(i) & 0xff;
        }

        @Override
        public void setElem(int bank, int i, int val) {
            buffer.put(i, (byte) val);
        }
    }

    static class UShortDataBuffer extends DataBuffer {
        private ShortBuffer buffer;

        UShortDataBuffer() {
            super(DataBuffer.TYPE_USHORT, 0, 0);
        }

        void setBuffer(ShortBuffer _buffer) {
            buffer = _buffer;
        }

        @Override
        public int getElem(int bank, int i) {
            return buffer.get(i) & 0xffff;
        }

        @Override
        public void setElem(int bank, int i, int val) {
            buffer.put(i, (short) val);
        }
    }

    static class IntDataBuffer extends DataBuffer {
        private IntBuffer buffer;

        IntDataBuffer() {
            super(DataBuffer.TYPE_INT, 0, 0);
        }

        void setBuffer(IntBuffer _buffer) {
            buffer = _buffer;
        }

        @Override
        public int getElem(int bank, int i) {
            return buffer.get(i);
        }

        @Override
        public void setElem(int bank, int i, int val) {
            buffer.put(i, val);
        }
    }

}
