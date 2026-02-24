package org.helioviewer.jhv.layers.filters;

import org.helioviewer.jhv.layers.ImageLayer;

public class BlendPanel extends AbstractSliderFilterPanel {

    public BlendPanel(ImageLayer layer) {
        super("Blend ",
                0,
                100,
                (int) (layer.getGLImage().getBlend() * 100),
                LevelsPanel::formatPercent,
                value -> layer.getGLImage().setBlend(value / 100.));
    }
}
