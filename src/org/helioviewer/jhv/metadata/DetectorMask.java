package org.helioviewer.jhv.metadata;

import java.io.InputStream;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.io.FileUtils;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.header.Bitpix;

public final class DetectorMask {

    private static final ImageBuffer BUILTIN_NONE = ImageBuffer.fromBytes(1, 1, ImageBuffer.Format.Gray8, new byte[]{(byte) 0xFF});
    private static final String EUI_OCCULTED_RESOURCE = "/data/eui_mask_2x2_1504.fits";

    public static final DetectorMask NONE = new DetectorMask(BUILTIN_NONE);
    public static DetectorMask EUI_OCCULTED = NONE;

    private final ImageBuffer imageBuffer;

    private DetectorMask(ImageBuffer _imageBuffer) {
        imageBuffer = _imageBuffer;
    }

    public static void loadBuiltins() {
        EUI_OCCULTED = new DetectorMask(loadImageBuffer(EUI_OCCULTED_RESOURCE));
    }

    public ImageBuffer getImageBuffer() {
        return imageBuffer;
    }

    private static ImageBuffer loadImageBuffer(String resourcePath) {
        try (InputStream in = FileUtils.getResource(resourcePath); Fits fits = new Fits(in)) {
            BasicHDU<?> hdu = fits.readHDU();
            if (!(hdu instanceof ImageHDU imageHDU) || hdu.getBitpix() != Bitpix.BYTE)
                throw new Exception("Detector mask must be an 8-bit FITS image");
            if (!(imageHDU.getKernel() instanceof byte[][] pixels) || pixels.length == 0 || pixels[0].length == 0)
                throw new Exception("Detector mask must be a non-empty 2D FITS image");

            int height = pixels.length;
            int width = pixels[0].length;
            ImageBuffer.WriteBuffer output = ImageBuffer.createWriteBuffer(width, height, ImageBuffer.Format.Gray8, ImageFilter.NONE);
            ByteBuffer outputPixels = output.byteBuffer();
            for (int y = 0; y < height; y++)
                outputPixels.put(width * (height - 1 - y), pixels[y], 0, width);
            return output.finish();
        } catch (Exception e) {
            Log.error("Cannot load detector mask " + resourcePath, e);
            return BUILTIN_NONE;
        }
    }

}
