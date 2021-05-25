package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;

import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;

public class SharpenPanel implements FilterDetails {

    private final JSlider slider;
    private final JLabel label;

    public SharpenPanel(ImageLayer layer) {
        slider = new JSlider(JSlider.HORIZONTAL, -100, 100, (int) (layer.getGLImage().getSharpen() * 100));
        label = new JLabel(LevelsPanel.align(slider.getValue()), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            int value = slider.getValue();
            layer.getGLImage().setSharpen(value / 100.);
            label.setText(LevelsPanel.align(value));
            MovieDisplay.display();
        });
        WheelSupport.installMouseWheelSupport(slider);
    }

    @Override
    public Component getTitle() {
        return new JLabel("Sharpen", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return slider;
    }

    @Override
    public Component getLabel() {
        return label;
    }

}
