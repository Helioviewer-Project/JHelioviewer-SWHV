package org.helioviewer.jhv.image.nio;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.IOException;

final class CompatibleImageUtils {

    @FunctionalInterface
    interface DataBufferFactory {
        DataBuffer create(int type, int size, int numBanks) throws IOException;
    }

    private CompatibleImageUtils() {}

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

    static BufferedImage createRGBAPremultipliedImage(int width, int height, DataBufferFactory dataBufferFactory) {
        try {
            return createRGBAPremultipliedImageOrThrow(width, height, dataBufferFactory);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected I/O creating RGBA premultiplied image", e);
        }
    }

    static BufferedImage createRGBAPremultipliedImageOrThrow(int width, int height, DataBufferFactory dataBufferFactory) throws IOException {
        ColorModel colorModel = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB),
                true,
                true,
                Transparency.TRANSLUCENT,
                DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = new PixelInterleavedSampleModel(
                DataBuffer.TYPE_BYTE,
                width,
                height,
                4,
                4 * width,
                new int[]{0, 1, 2, 3});
        DataBuffer buffer = dataBufferFactory.create(DataBuffer.TYPE_BYTE, 4 * width * height, 1);
        return new BufferedImage(colorModel, Raster.createWritableRaster(sampleModel, buffer, null), true, null);
    }

    static BufferedImage createRGBImage(int width, int height, DataBufferFactory dataBufferFactory) {
        try {
            return createRGBImageOrThrow(width, height, dataBufferFactory);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected I/O creating RGB image", e);
        }
    }

    static BufferedImage createRGBImageOrThrow(int width, int height, DataBufferFactory dataBufferFactory) throws IOException {
        ColorModel colorModel = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB),
                false,
                false,
                Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = new PixelInterleavedSampleModel(
                DataBuffer.TYPE_BYTE,
                width,
                height,
                3,
                3 * width,
                new int[]{0, 1, 2});
        DataBuffer buffer = dataBufferFactory.create(DataBuffer.TYPE_BYTE, 3 * width * height, 1);
        return new BufferedImage(colorModel, Raster.createWritableRaster(sampleModel, buffer, null), false, null);
    }
}
