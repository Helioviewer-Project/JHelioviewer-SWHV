package org.helioviewer.jhv.image.nio;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MappedImageFactory {

    private static final AbstractOwnedDataBuffer.BackendKind BACKEND_KIND = AbstractOwnedDataBuffer.BackendKind.MAPPED_FILE;

    public static BufferedImage createRGBImage(int width, int height) throws IOException {
        return CompatibleImageUtils.createRGBImageOrThrow(width, height,
                (dataType, size, numBanks) -> AbstractOwnedDataBuffer.createOrThrow(dataType, size, numBanks, BACKEND_KIND, BufferBacking::mapFile));
    }

    public static ByteBuffer getByteBuffer(BufferedImage bi) {
        return AbstractOwnedDataBuffer.getByteBuffer(bi, BACKEND_KIND);
    }

    public static void free(BufferedImage bi) {
        AbstractOwnedDataBuffer.free(bi, BACKEND_KIND);
    }

    private MappedImageFactory() {}
}
