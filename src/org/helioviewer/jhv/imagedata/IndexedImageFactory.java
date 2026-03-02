package org.helioviewer.jhv.imagedata;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.imagedata.nio.GenericWritableRaster;

public class IndexedImageFactory {

    private IndexedImageFactory() {
    }

    public static BufferedImage createIndexed(Buffer buffer, int width, int height, IndexColorModel cm) {
        BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_INDEXED, cm);
        SampleModel sm = temp.getSampleModel().createCompatibleSampleModel(width, height);
        return new BufferedImage(cm, new GenericWritableRaster(sm, createDataBuffer(buffer), new Point()), false, null);
    }

    private static DataBuffer createDataBuffer(Buffer buffer) {
        switch (buffer) {
            case ByteBuffer byteBuffer -> {
                ByteDataBuffer ret = new ByteDataBuffer();
                ret.setBuffer(byteBuffer);
                return ret;
            }
            case ShortBuffer shortBuffer -> {
                UShortDataBuffer ret = new UShortDataBuffer();
                ret.setBuffer(shortBuffer);
                return ret;
            }
            case IntBuffer intBuffer -> {
                IntDataBuffer ret = new IntDataBuffer();
                ret.setBuffer(intBuffer);
                return ret;
            }
            case null, default -> throw new IllegalArgumentException("Unsupported data type: " + buffer);
        }
    }

    private static class ByteDataBuffer extends DataBuffer {
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

    private static class UShortDataBuffer extends DataBuffer {
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

    private static class IntDataBuffer extends DataBuffer {
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
