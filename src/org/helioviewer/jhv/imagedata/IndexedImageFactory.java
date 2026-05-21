package org.helioviewer.jhv.imagedata;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class IndexedImageFactory {

    private IndexedImageFactory() {}

    public static BufferedImage createIndexed(Buffer buffer, int width, int height, IndexColorModel cm) {
        BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_INDEXED, cm);
        SampleModel sm = temp.getSampleModel().createCompatibleSampleModel(width, height);
        return new BufferedImage(cm, Raster.createWritableRaster(sm, createDataBuffer(buffer), null), false, null);
    }

    private static DataBuffer createDataBuffer(Buffer buffer) {
        switch (buffer) {
            case ByteBuffer byteBuffer -> {
                return new ByteDataBuffer(byteBuffer);
            }
            case ShortBuffer shortBuffer -> {
                return new UShortDataBuffer(shortBuffer);
            }
            case IntBuffer intBuffer -> {
                return new IntDataBuffer(intBuffer);
            }
            case null, default -> throw new IllegalArgumentException("Unsupported data type: " + buffer);
        }
    }

    private static class ByteDataBuffer extends DataBuffer {
        private final ByteBuffer buffer;

        ByteDataBuffer(ByteBuffer _buffer) {
            super(DataBuffer.TYPE_BYTE, _buffer.limit());
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
        private final ShortBuffer buffer;

        UShortDataBuffer(ShortBuffer _buffer) {
            super(DataBuffer.TYPE_USHORT, _buffer.limit());
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
        private final IntBuffer buffer;

        IntDataBuffer(IntBuffer _buffer) {
            super(DataBuffer.TYPE_INT, _buffer.limit());
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
