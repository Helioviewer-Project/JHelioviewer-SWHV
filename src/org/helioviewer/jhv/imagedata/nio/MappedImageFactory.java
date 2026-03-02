package org.helioviewer.jhv.imagedata.nio;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
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
        BufferedImage temp = new BufferedImage(1, 1, type);
        SampleModel sampleModel = temp.getSampleModel().createCompatibleSampleModel(width, height);
        ColorModel colorModel = temp.getColorModel();
        DataBuffer buffer = AbstractOwnedDataBuffer.createOrThrow(
                sampleModel.getTransferType(),
                width * height * sampleModel.getNumDataElements(),
                1,
                BACKEND_KIND,
                BufferBacking::mapFile);
        return new BufferedImage(colorModel, new GenericWritableRaster(sampleModel, buffer, new Point()), colorModel.isAlphaPremultiplied(), null);
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
