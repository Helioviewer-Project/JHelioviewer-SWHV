package org.helioviewer.jhv.layers.filters;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.opengl.GLImage;

public class DeltaCRVAL1Panel extends AbstractSliderFilterPanel {

    public DeltaCRVAL1Panel(ImageLayer layer) {
        super("\u03B4CRVAL1",
                GLImage.MIN_DCRVAL,
                GLImage.MAX_DCRVAL,
                layer.getGLImage().getDeltaCRVAL1(),
                DeltaCRVAL1Panel::formatInt,
                layer.getGLImage()::setDeltaCRVAL1);
    }

    private static String formatInt(int value) {
        return "<html><p align='right'>" + value + "\u2033</p>";
    }
}
