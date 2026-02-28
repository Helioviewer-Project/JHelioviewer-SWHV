package org.helioviewer.jhv.imagedata.nio;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MappedImageFactory {

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
        return AbstractOwnedDataBuffer.createCompatibleImageOrThrow(width, height, type, MappedFileBuffer::create);
    }

    public static ByteBuffer getByteBuffer(BufferedImage bi) {
        return MappedFileBuffer.getByteBuffer(bi.getRaster().getDataBuffer());
    }

    public static void free(BufferedImage bi) {
        if (bi == null)
            return;
        MappedFileBuffer.free(bi.getRaster().getDataBuffer());
    }

}
