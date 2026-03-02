package org.helioviewer.jhv.imagedata.nio;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.io.IOException;
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
        try {
            return createCompatibleOrThrow(width, height, type);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected I/O creating image", e);
        }
    }

    public static ByteBuffer getByteBuffer(BufferedImage bi) {
        return AbstractOwnedDataBuffer.getByteBuffer(bi.getRaster().getDataBuffer(), BACKEND_KIND);
    }

    public static void free(BufferedImage bi) {
        if (bi == null)
            return;
        AbstractOwnedDataBuffer.free(bi.getRaster().getDataBuffer(), BACKEND_KIND);
    }

    private static BufferedImage createCompatibleOrThrow(int width, int height, int type) throws IOException {
        BufferedImage temp = new BufferedImage(1, 1, type);
        SampleModel sampleModel = temp.getSampleModel().createCompatibleSampleModel(width, height);
        ColorModel colorModel = temp.getColorModel();
        DataBuffer buffer = AbstractOwnedDataBuffer.create(
                sampleModel.getTransferType(),
                width * height * sampleModel.getNumDataElements(),
                1,
                BACKEND_KIND,
                BufferBacking::allocate);
        return new BufferedImage(colorModel, new GenericWritableRaster(sampleModel, buffer, new Point()), colorModel.isAlphaPremultiplied(), null);
    }

}
