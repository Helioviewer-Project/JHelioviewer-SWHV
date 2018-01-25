package org.helioviewer.jhv.base.image;

import java.awt.image.DataBuffer;
import java.nio.*;

import org.helioviewer.jhv.base.BufferUtils;

public abstract class NIODataBuffer extends DataBuffer {
    private final Buffer buffer;

    private NIODataBuffer(int type, int size, int numBanks) {
        super(type, size, numBanks);

        int componentSize = DataBuffer.getDataTypeSize(type) / 8;
        int length = size * componentSize * numBanks;

        ByteBuffer byteBuffer = BufferUtils.newByteBuffer(length);
        switch (type) {
            case DataBuffer.TYPE_BYTE:
                buffer = byteBuffer;
                break;
            case DataBuffer.TYPE_USHORT:
                buffer = byteBuffer.asShortBuffer();
                break;
            case DataBuffer.TYPE_INT:
                buffer = byteBuffer.asIntBuffer();
                break;
            default:
                throw new IllegalArgumentException("Unsupported data type: " + type);
        }
    }

    Buffer getBuffer() {
        return buffer;
    }

    @Override
    public String toString() {
        return String.format("NIODataBuffer: %s", buffer);
    }

    public static DataBuffer create(int type, int size, int numBanks) {
        switch (type) {
            case DataBuffer.TYPE_BYTE:
                return new DataBufferByte(size, numBanks);
            case DataBuffer.TYPE_USHORT:
                return new DataBufferUShort(size, numBanks);
            case DataBuffer.TYPE_INT:
                return new DataBufferInt(size, numBanks);
            default:
                throw new IllegalArgumentException("Unsupported data type: " + type);
        }
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

    public static class DataBufferByte extends NIODataBuffer {
        private final ByteBuffer buffer;

        public DataBufferByte(int size, int numBanks) {
            super(DataBuffer.TYPE_BYTE, size, numBanks);
            buffer = (ByteBuffer) super.buffer;
        }

        @Override
        public ByteBuffer getBuffer() {
            return buffer;
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

    public static class DataBufferUShort extends NIODataBuffer {
        private final ShortBuffer buffer;

        public DataBufferUShort(int size, int numBanks) {
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

    public static class DataBufferInt extends NIODataBuffer {
        private final IntBuffer buffer;

        public DataBufferInt(int size, int numBanks) {
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

    public static class ByteDataBuffer extends DataBuffer {
        private ByteBuffer buffer;

        ByteDataBuffer() {
            super(DataBuffer.TYPE_BYTE, 0, 0);
        }

        ByteDataBuffer setBuffer(ByteBuffer _buffer) {
            buffer = _buffer;
            return this;
        }

        public ByteBuffer getBuffer() {
            return buffer;
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

    public static class UShortDataBuffer extends DataBuffer {
        private ShortBuffer buffer;

        UShortDataBuffer() {
            super(DataBuffer.TYPE_USHORT, 0, 0);
        }

        UShortDataBuffer setBuffer(ShortBuffer _buffer) {
            buffer = _buffer;
            return this;
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

    public static class IntDataBuffer extends DataBuffer {
        private IntBuffer buffer;

        IntDataBuffer() {
            super(DataBuffer.TYPE_INT, 0, 0);
        }

        IntDataBuffer setBuffer(IntBuffer _buffer) {
            buffer = _buffer;
            return this;
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
