package org.helioviewer.jhv.layers.filters;

import org.helioviewer.jhv.layers.ImageLayer;

public class OpacityPanel extends AbstractSliderFilterPanel {

    public OpacityPanel(ImageLayer layer) {
        super("Opacity ",
                0,
                100,
                (int) (layer.getGLImage().getOpacity() * 100),
                LevelsPanel::formatPercent,
                value -> layer.getGLImage().setOpacity(value / 100.));
    }
}
