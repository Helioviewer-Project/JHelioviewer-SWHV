package org.helioviewer.jhv.layers.filters;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.opengl.GLImage;

public class DeltaCROTAPanel extends AbstractSliderFilterPanel {

    public DeltaCROTAPanel(ImageLayer layer) {
        super("\u03B4CROTA",
                GLImage.MIN_DCROTA * 10,
                GLImage.MAX_DCROTA * 10,
                (int) (layer.getGLImage().getDeltaCROTA() * 10),
                value -> formatFloat(value / 10.0),
                value -> layer.getGLImage().setDeltaCROTA(value / 10.0));
    }

    private static String formatFloat(double value) {
        return "<html><p align='right'>" + String.format("%.1f", value) + "\u00B0</p>";
    }
}
