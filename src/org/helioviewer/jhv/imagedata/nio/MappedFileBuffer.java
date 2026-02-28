package org.helioviewer.jhv.imagedata.nio;

import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.ByteBuffer;

final class MappedFileBuffer {

    private static final AbstractOwnedDataBuffer.BackendKind BACKEND_KIND = AbstractOwnedDataBuffer.BackendKind.MAPPED_FILE;

    private MappedFileBuffer() {
    }

    static DataBuffer create(int type, int size, int numBanks) throws IOException {
        return AbstractOwnedDataBuffer.createOrThrow(type, size, numBanks, BACKEND_KIND, BufferBacking::mapFile);
    }

    static ByteBuffer getByteBuffer(DataBuffer buffer) {
        return AbstractOwnedDataBuffer.getByteBuffer(buffer, BACKEND_KIND);
    }

    static void free(DataBuffer buffer) {
        AbstractOwnedDataBuffer.free(buffer, BACKEND_KIND);
    }

}
