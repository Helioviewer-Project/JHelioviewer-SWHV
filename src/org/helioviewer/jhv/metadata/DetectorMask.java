package org.helioviewer.jhv.metadata;

import java.io.InputStream;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.view.uri.FITSImage;

public final class DetectorMask {

    private static final ImageBuffer BUILTIN_NONE = new ImageBuffer(
            1, 1, ImageBuffer.Format.Gray8,
            BufferUtils.newByteBuffer(1).put((byte) 0xFF).flip());

    public static final DetectorMask NONE = new DetectorMask("none", null);
    public static final DetectorMask EUI_OCCULTED = new DetectorMask("/data/eui_mask_2x2_1504.fits", BUILTIN_NONE);

    private final String resourcePath;
    private final ImageBuffer fallback;
    private final Object lock = new Object();
    private volatile ImageBuffer imageBuffer;

    private DetectorMask(String _resourcePath, ImageBuffer _fallback) {
        resourcePath = _resourcePath;
        fallback = _fallback;
    }

    @Nonnull
    public ImageBuffer getImageBuffer() {
        ImageBuffer current = imageBuffer;
        if (current != null)
            return current;
        synchronized (lock) {
            current = imageBuffer;
            if (current != null)
                return current;

            imageBuffer = loadImageBuffer();
            return imageBuffer;
        }
    }

    private ImageBuffer loadImageBuffer() {
        if (fallback == null)
            return BUILTIN_NONE;

        try (InputStream in = FileUtils.getResource(resourcePath)) {
            return new FITSImage().readImageBuffer(in);
        } catch (Exception e) {
            Log.error("Cannot load detector mask " + resourcePath, e);
            return fallback;
        }
    }

}
