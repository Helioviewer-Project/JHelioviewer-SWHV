package org.helioviewer.jhv.imagedata.nio;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

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

    public static ByteBuffer getByteBuffer(BufferedImage bi) {
        return AbstractOwnedDataBuffer.getByteBuffer(bi.getRaster().getDataBuffer(), BACKEND_KIND);
    }

    public static void free(BufferedImage bi) {
        if (bi == null)
            return;
        AbstractOwnedDataBuffer.free(bi.getRaster().getDataBuffer(), BACKEND_KIND);
    }

}
