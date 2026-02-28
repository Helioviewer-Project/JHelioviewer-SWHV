package org.helioviewer.jhv.imagedata.nio;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class NativeImageFactory {

    private NativeImageFactory() {
    }

    public static BufferedImage copyImage(BufferedImage bi) {
        BufferedImage ret = AbstractOwnedDataBuffer.createCompatibleImage(bi.getWidth(), bi.getHeight(), bi.getType(), NativeDataBuffer::create);
        try {
            bi.copyData(ret.getRaster());
            return ret;
        } catch (RuntimeException | Error e) {
            free(ret);
            throw e;
        }
    }

    public static BufferedImage createCompatible(int width, int height, int type) {
        return AbstractOwnedDataBuffer.createCompatibleImage(width, height, type, NativeDataBuffer::create);
    }

    public static ByteBuffer getByteBuffer(BufferedImage bi) {
        return NativeDataBuffer.getByteBuffer(bi.getRaster().getDataBuffer());
    }

    public static void free(BufferedImage bi) {
        if (bi == null)
            return;
        NativeDataBuffer.free(bi.getRaster().getDataBuffer());
    }

}
