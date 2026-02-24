package org.helioviewer.jhv.layers.filters;

import org.helioviewer.jhv.layers.ImageLayer;

public class SharpenPanel extends AbstractSliderFilterPanel {

    public SharpenPanel(ImageLayer layer) {
        super("Sharpen ",
                -100,
                100,
                (int) (layer.getGLImage().getSharpen() * 100),
                LevelsPanel::formatPercent,
                value -> layer.getGLImage().setSharpen(value / 100.));
    }
}
