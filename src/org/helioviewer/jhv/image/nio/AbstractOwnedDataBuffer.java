package org.helioviewer.jhv.image.nio;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
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

    private AbstractOwnedDataBuffer(int type, int size, BufferBacking _backing, BackendKind _backendKind) {
        super(type, size);
        backing = _backing;
        buffer = _backing.buffer();
        backendKind = _backendKind;
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
        BufferBacking create(int type, int size) throws IOException;
    }

    static DataBuffer createOrThrow(int type, int size, BackendKind backendKind, BackingFactory backingFactory) throws IOException {
        return switch (type) {
            case DataBuffer.TYPE_BYTE -> new ByteDataBuffer(size, backingFactory.create(DataBuffer.TYPE_BYTE, size), backendKind);
            case DataBuffer.TYPE_USHORT -> new UShortDataBuffer(size, backingFactory.create(DataBuffer.TYPE_USHORT, size), backendKind);
            case DataBuffer.TYPE_INT -> new IntDataBuffer(size, backingFactory.create(DataBuffer.TYPE_INT, size), backendKind);
            default -> throw new IllegalArgumentException("Unsupported data type: " + type);
        };
    }

    static ByteBuffer getByteBuffer(DataBuffer buffer, BackendKind backendKind) {
        if (buffer instanceof ByteDataBuffer byteDataBuffer && byteDataBuffer.isKind(backendKind))
            return byteDataBuffer.byteBuffer();
        throw new IncompatibleClassChangeError("Not a " + backendKind + " byte backed image");
    }

    static ByteBuffer getByteBuffer(BufferedImage image, BackendKind backendKind) {
        return getByteBuffer(image.getRaster().getDataBuffer(), backendKind);
    }

    static void free(DataBuffer buffer, BackendKind backendKind) {
        if (buffer instanceof AbstractOwnedDataBuffer ownedDataBuffer && ownedDataBuffer.isKind(backendKind))
            ownedDataBuffer.free();
    }

    static void free(BufferedImage image, BackendKind backendKind) {
        if (image == null)
            return;
        free(image.getRaster().getDataBuffer(), backendKind);
    }

    private static final class ByteDataBuffer extends AbstractOwnedDataBuffer {
        private ByteDataBuffer(int size, BufferBacking backing, BackendKind backendKind) {
            super(DataBuffer.TYPE_BYTE, size, backing, backendKind);
        }

        @Override
        public int getElem(int bank, int i) {
            return byteBuffer().get(bank * size + i) & 0xff;
        }

        @Override
        public void setElem(int bank, int i, int val) {
            byteBuffer().put(bank * size + i, (byte) val);
        }
    }

    private static final class UShortDataBuffer extends AbstractOwnedDataBuffer {
        private UShortDataBuffer(int size, BufferBacking backing, BackendKind backendKind) {
            super(DataBuffer.TYPE_USHORT, size, backing, backendKind);
        }

        @Override
        public int getElem(int bank, int i) {
            return shortBuffer().get(bank * size + i) & 0xffff;
        }

        @Override
        public void setElem(int bank, int i, int val) {
            shortBuffer().put(bank * size + i, (short) val);
        }
    }

    private static final class IntDataBuffer extends AbstractOwnedDataBuffer {
        private IntDataBuffer(int size, BufferBacking backing, BackendKind backendKind) {
            super(DataBuffer.TYPE_INT, size, backing, backendKind);
        }

        @Override
        public int getElem(int bank, int i) {
            return intBuffer().get(bank * size + i);
        }

        @Override
        public void setElem(int bank, int i, int val) {
            intBuffer().put(bank * size + i, val);
        }
    }

}
