package org.helioviewer.jhv.plugins.swek;

import java.awt.image.BufferedImage;
import java.util.HashMap;

public final class SWEKIconImages {

    private static final HashMap<String, BufferedImage> images = new HashMap<>();

    public static BufferedImage getImage(String iconName) {
        return images.computeIfAbsent(SWEKIconBank.resource(iconName), ignored -> {
            SWEKIconBank.Pixels pixels = SWEKIconBank.getPixels(iconName);
            BufferedImage image = new BufferedImage(pixels.width(), pixels.height(), BufferedImage.TYPE_4BYTE_ABGR_PRE);
            // Raster elements are RGBA; the standard image stores them as ABGR.
            image.getRaster().setDataElements(0, 0, pixels.width(), pixels.height(), pixels.rgba());
            return image;
        });
    }

    private SWEKIconImages() {}
}
