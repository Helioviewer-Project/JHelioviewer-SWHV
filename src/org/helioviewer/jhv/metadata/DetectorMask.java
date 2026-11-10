package org.helioviewer.jhv.metadata;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.source.FITSReader;

import nom.tam.fits.Header;
import nom.tam.util.FitsInputStream;

public final class DetectorMask {

    private static final ImageBuffer BUILTIN_NONE = ImageBuffer.fromBytes(1, 1, new byte[]{(byte) 0xFF});
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
        try (InputStream in = FileUtils.getResource(resourcePath)) {
            byte[] bytes = in.readAllBytes();
            Header header;
            try (FitsInputStream input = new FitsInputStream(new ByteArrayInputStream(bytes))) {
                header = Header.readHeader(input);
            }
            if (header == null || header.getIntValue("BITPIX") != 8 || header.getIntValue("NAXIS") != 2)
                throw new Exception("Detector mask must be an 8-bit FITS image");
            int height = header.getIntValue("NAXIS2");
            int width = header.getIntValue("NAXIS1");
            if (width <= 0 || height <= 0)
                throw new Exception("Detector mask must be a non-empty 2D FITS image");
            byte[] pixels = FITSReader.readMask(bytes, width, height);
            try (ImageBuffer.WriteBuffer output = ImageBuffer.createWriteBuffer(width, height, ImageBuffer.Format.Gray8)) {
                ByteBuffer outputPixels = output.byteBuffer();
                for (int y = 0; y < height; y++)
                    outputPixels.put(width * (height - 1 - y), pixels, width * y, width);
                return output.finish();
            }
        } catch (Exception e) {
            Log.error("Cannot load detector mask " + resourcePath, e);
            return BUILTIN_NONE;
        }
    }

}
