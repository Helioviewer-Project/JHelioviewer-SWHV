package org.helioviewer.jhv.imagedata.nio;

import java.awt.image.DataBuffer;
import java.nio.ByteBuffer;

final class NativeDataBuffer {

    private static final AbstractOwnedDataBuffer.BackendKind BACKEND_KIND = AbstractOwnedDataBuffer.BackendKind.NATIVE;

    private NativeDataBuffer() {
    }

    static DataBuffer create(int type, int size, int numBanks) {
        return AbstractOwnedDataBuffer.create(type, size, numBanks, BACKEND_KIND, BufferBacking::allocate);
    }

    static ByteBuffer getByteBuffer(DataBuffer buffer) {
        return AbstractOwnedDataBuffer.getByteBuffer(buffer, BACKEND_KIND);
    }

    static void free(DataBuffer buffer) {
        AbstractOwnedDataBuffer.free(buffer, BACKEND_KIND);
    }

}
