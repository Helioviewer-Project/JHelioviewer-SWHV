package org.helioviewer.jhv.imagedata.nio;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

abstract class AbstractOwnedDataBuffer extends DataBuffer {

    enum BackendKind {NATIVE, MAPPED_FILE}

    private Buffer buffer;
    private BufferBacking backing;
    private final BackendKind backendKind;

    private AbstractOwnedDataBuffer(int type, int size, int numBanks, BufferBacking _backing, BackendKind _backendKind) {
        super(type, size, numBanks);
        backing = _backing;
        buffer = _backing.buffer();
        backendKind = _backendKind;
    }

    final Buffer getBuffer() {
        return requireBuffer();
    }

    final void free() {
        if (backing == null)
            return;

        backing.close();
        backing = null;
        buffer = null;
    }

    final boolean isKind(BackendKind expectedBackendKind) {
        return backendKind == expectedBackendKind;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", backendKind, buffer);
    }

    private Buffer requireBuffer() {
        if (buffer == null)
            throw new IllegalStateException(backendKind + " buffer is unavailable");
        return buffer;
    }

    protected final ByteBuffer byteBuffer() {
        return (ByteBuffer) requireBuffer();
    }

    protected final ShortBuffer shortBuffer() {
        return (ShortBuffer) requireBuffer();
    }

    protected final IntBuffer intBuffer() {
        return (IntBuffer) requireBuffer();
    }

    @FunctionalInterface
    interface BackingFactory {
        BufferBacking create(int type, int size, int numBanks) throws IOException;
    }

    static DataBuffer create(int type, int size, int numBanks, BackendKind backendKind, BackingFactory backingFactory) {
        try {
            return createOrThrow(type, size, numBanks, backendKind, backingFactory);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected I/O creating " + backendKind + " buffer", e);
        }
    }

    static DataBuffer createOrThrow(int type, int size, int numBanks, BackendKind backendKind, BackingFactory backingFactory) throws IOException {
        return switch (type) {
            case DataBuffer.TYPE_BYTE ->
                    new OwnedByteDataBuffer(size, numBanks, backingFactory.create(DataBuffer.TYPE_BYTE, size, numBanks), backendKind);
            case DataBuffer.TYPE_USHORT ->
                    new OwnedUShortDataBuffer(size, numBanks, backingFactory.create(DataBuffer.TYPE_USHORT, size, numBanks), backendKind);
            case DataBuffer.TYPE_INT ->
                    new OwnedIntDataBuffer(size, numBanks, backingFactory.create(DataBuffer.TYPE_INT, size, numBanks), backendKind);
            default -> throw new IllegalArgumentException("Unsupported data type: " + type);
        };
    }

    static ByteBuffer getByteBuffer(DataBuffer buffer, BackendKind backendKind) {
        if (buffer instanceof ByteDataBuffer byteDataBuffer && byteDataBuffer.isKind(backendKind))
            return (ByteBuffer) byteDataBuffer.getBuffer();
        throw new IncompatibleClassChangeError("Not a " + backendKind + " byte backed image");
    }

    static void free(DataBuffer buffer, BackendKind backendKind) {
        if (buffer instanceof AbstractOwnedDataBuffer ownedDataBuffer && ownedDataBuffer.isKind(backendKind))
            ownedDataBuffer.free();
    }

    @FunctionalInterface
    interface DataBufferFactory {
        DataBuffer create(int type, int size, int numBanks) throws IOException;
    }

    static BufferedImage createCompatibleImage(int width, int height, int type, DataBufferFactory dataBufferFactory) {
        try {
            return createCompatibleImageOrThrow(width, height, type, dataBufferFactory);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected I/O creating image", e);
        }
    }

    static BufferedImage createCompatibleImageOrThrow(int width, int height, int type, DataBufferFactory dataBufferFactory) throws IOException {
        BufferedImage temp = new BufferedImage(1, 1, type);
        SampleModel sampleModel = temp.getSampleModel().createCompatibleSampleModel(width, height);
        ColorModel colorModel = temp.getColorModel();
        DataBuffer buffer = dataBufferFactory.create(sampleModel.getTransferType(), width * height * sampleModel.getNumDataElements(), 1);
        return new BufferedImage(colorModel, RasterFactory.factory.createRaster(sampleModel, buffer, new Point()), colorModel.isAlphaPremultiplied(), null);
    }

    private abstract static class ByteDataBuffer extends AbstractOwnedDataBuffer {
        private ByteDataBuffer(int size, int numBanks, BufferBacking backing, BackendKind backendKind) {
            super(DataBuffer.TYPE_BYTE, size, numBanks, backing, backendKind);
        }

        @Override
        public final int getElem(int bank, int i) {
            return byteBuffer().get(bank * size + i) & 0xff;
        }

        @Override
        public final void setElem(int bank, int i, int val) {
            byteBuffer().put(bank * size + i, (byte) val);
        }
    }

    private abstract static class UShortDataBuffer extends AbstractOwnedDataBuffer {
        private UShortDataBuffer(int size, int numBanks, BufferBacking backing, BackendKind backendKind) {
            super(DataBuffer.TYPE_USHORT, size, numBanks, backing, backendKind);
        }

        @Override
        public final int getElem(int bank, int i) {
            return shortBuffer().get(bank * size + i) & 0xffff;
        }

        @Override
        public final void setElem(int bank, int i, int val) {
            shortBuffer().put(bank * size + i, (short) val);
        }
    }

    private abstract static class IntDataBuffer extends AbstractOwnedDataBuffer {
        private IntDataBuffer(int size, int numBanks, BufferBacking backing, BackendKind backendKind) {
            super(DataBuffer.TYPE_INT, size, numBanks, backing, backendKind);
        }

        @Override
        public final int getElem(int bank, int i) {
            return intBuffer().get(bank * size + i);
        }

        @Override
        public final void setElem(int bank, int i, int val) {
            intBuffer().put(bank * size + i, val);
        }
    }

    private static final class OwnedByteDataBuffer extends ByteDataBuffer {
        private OwnedByteDataBuffer(int size, int numBanks, BufferBacking backing, BackendKind backendKind) {
            super(size, numBanks, backing, backendKind);
        }
    }

    private static final class OwnedUShortDataBuffer extends UShortDataBuffer {
        private OwnedUShortDataBuffer(int size, int numBanks, BufferBacking backing, BackendKind backendKind) {
            super(size, numBanks, backing, backendKind);
        }
    }

    private static final class OwnedIntDataBuffer extends IntDataBuffer {
        private OwnedIntDataBuffer(int size, int numBanks, BufferBacking backing, BackendKind backendKind) {
            super(size, numBanks, backing, backendKind);
        }
    }

}
