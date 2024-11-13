package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.base.JHVSlider;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;

public class SharpenPanel implements FilterDetails {

    private final JHVSlider slider;
    private final JLabel label;
    private final JLabel title = new JLabel("Sharpen ", JLabel.RIGHT);

    public SharpenPanel(ImageLayer layer) {
        slider = new JHVSlider(-100, 100, (int) (layer.getGLImage().getSharpen() * 100));
        label = new JLabel(LevelsPanel.formatPercent(slider.getValue()), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            int value = slider.getValue();
            layer.getGLImage().setSharpen(value / 100.);
            label.setText(LevelsPanel.formatPercent(value));
            MovieDisplay.display();
        });
    }

    @Override
    public Component getFirst() {
        return title;
    }

    @Override
    public Component getSecond() {
        return slider;
    }

    @Override
    public Component getThird() {
        return label;
    }

}
