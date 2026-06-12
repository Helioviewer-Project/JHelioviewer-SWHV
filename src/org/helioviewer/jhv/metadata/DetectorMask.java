package org.helioviewer.jhv.metadata;

import java.io.InputStream;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.view.uri.FITSImage;

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
        try (InputStream in = FileUtils.getResource(resourcePath)) {
            return new FITSImage().readImageBuffer(in);
        } catch (Exception e) {
            Log.error("Cannot load detector mask " + resourcePath, e);
            return BUILTIN_NONE;
        }
    }

}
