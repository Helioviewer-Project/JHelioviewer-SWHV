package org.helioviewer.jhv.imagedata.nio;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.IOException;

final class CompatibleImageUtils {

    @FunctionalInterface
    interface DataBufferFactory {
        DataBuffer create(int type, int size, int numBanks) throws IOException;
    }

    private CompatibleImageUtils() {
    }

    static BufferedImage createCompatibleImage(int width, int height, int type, DataBufferFactory dataBufferFactory) {
        try {
            return createCompatibleImageOrThrow(width, height, type, dataBufferFactory);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected I/O creating image", e);
        }
    }

    static BufferedImage createCompatibleImageOrThrow(int width, int height, int type, DataBufferFactory dataBufferFactory) throws IOException {
        BufferedImage temp = new BufferedImage(1, 1, type);
        SampleModel sampleModel = temp.getSampleModel().createCompatibleSampleModel(width, height);
        ColorModel colorModel = temp.getColorModel();
        DataBuffer buffer = dataBufferFactory.create(
                sampleModel.getTransferType(),
                width * height * sampleModel.getNumDataElements(),
                1);
        return new BufferedImage(colorModel, Raster.createWritableRaster(sampleModel, buffer, null), colorModel.isAlphaPremultiplied(), null);
    }
}
