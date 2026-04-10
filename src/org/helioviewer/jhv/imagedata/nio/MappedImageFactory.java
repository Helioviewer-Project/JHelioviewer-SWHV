package org.helioviewer.jhv.imagedata.nio;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class MappedImageFactory {

    private static final AbstractOwnedDataBuffer.BackendKind BACKEND_KIND = AbstractOwnedDataBuffer.BackendKind.MAPPED_FILE;

    private MappedImageFactory() {
    }

    /*
        public static BufferedImage copyImage(BufferedImage bi) throws IOException {
            BufferedImage ret = createCompatible(bi.getWidth(), bi.getHeight(), bi.getType());
            bi.copyData(ret.getRaster());
            return ret;
        }
    */
    public static BufferedImage createCompatible(int width, int height, int type) throws IOException {
        return CompatibleImageUtils.createCompatibleImageOrThrow(
                width, height, type,
                (dataType, size, numBanks) -> AbstractOwnedDataBuffer.createOrThrow(dataType, size, numBanks, BACKEND_KIND, BufferBacking::mapFile));
    }

    public static BufferedImage createRGBAPremultipliedImage(int width, int height) throws IOException {
        return CompatibleImageUtils.createRGBAPremultipliedImageOrThrow(
                width, height,
                (dataType, size, numBanks) -> AbstractOwnedDataBuffer.createOrThrow(dataType, size, numBanks, BACKEND_KIND, BufferBacking::mapFile));
    }

    public static ByteBuffer getByteBuffer(BufferedImage bi) {
        return AbstractOwnedDataBuffer.getByteBuffer(bi, BACKEND_KIND);
    }

    public static ShortBuffer getShortBuffer(BufferedImage bi) {
        return AbstractOwnedDataBuffer.getShortBuffer(bi, BACKEND_KIND);
    }

    public static IntBuffer getIntBuffer(BufferedImage bi) {
        return AbstractOwnedDataBuffer.getIntBuffer(bi, BACKEND_KIND);
    }

    public static void free(BufferedImage bi) {
        AbstractOwnedDataBuffer.free(bi, BACKEND_KIND);
    }

}
