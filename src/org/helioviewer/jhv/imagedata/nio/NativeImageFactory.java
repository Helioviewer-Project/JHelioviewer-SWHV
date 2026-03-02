package org.helioviewer.jhv.imagedata.nio;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class NativeImageFactory {

    private static final AbstractOwnedDataBuffer.BackendKind BACKEND_KIND = AbstractOwnedDataBuffer.BackendKind.NATIVE;

    private NativeImageFactory() {
    }

    public static BufferedImage copyImage(BufferedImage bi) {
        BufferedImage ret = createCompatible(bi.getWidth(), bi.getHeight(), bi.getType());
        try {
            bi.copyData(ret.getRaster());
            return ret;
        } catch (RuntimeException | Error e) {
            free(ret);
            throw e;
        }
    }

    public static BufferedImage createCompatible(int width, int height, int type) {
        return CompatibleImageUtils.createCompatibleImage(
                width, height, type,
                (dataType, size, numBanks) -> AbstractOwnedDataBuffer.create(dataType, size, numBanks, BACKEND_KIND, BufferBacking::allocate));
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
