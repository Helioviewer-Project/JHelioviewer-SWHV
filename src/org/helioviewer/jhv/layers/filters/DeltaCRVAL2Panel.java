package org.helioviewer.jhv.layers.filters;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.opengl.GLImage;

public class DeltaCRVAL2Panel extends AbstractSliderFilterPanel {

    public DeltaCRVAL2Panel(ImageLayer layer) {
        super("\u03B4CRVAL2",
                GLImage.MIN_DCRVAL,
                GLImage.MAX_DCRVAL,
                layer.getGLImage().getDeltaCRVAL2(),
                DeltaCRVAL2Panel::formatInt,
                layer.getGLImage()::setDeltaCRVAL2);
    }

    private static String formatInt(int value) {
        return "<html><p align='right'>" + value + "\u2033</p>";
    }
}
