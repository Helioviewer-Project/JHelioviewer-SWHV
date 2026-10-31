package org.helioviewer.jhv.plugins.swek;

import java.awt.image.BufferedImage;
import java.util.HashMap;

public final class SWEKIconImages {

    private static final HashMap<String, BufferedImage> images = new HashMap<>();

    public static BufferedImage getImage(String iconName) {
        return images.computeIfAbsent(SWEKIconBank.resource(iconName), ignored -> SWEKIconBank.getPixels(iconName).image());
    }

    private SWEKIconImages() {}
}
