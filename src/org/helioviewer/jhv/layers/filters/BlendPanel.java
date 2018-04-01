package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayer;

public class BlendPanel implements FilterDetails {

    private final JSlider slider;
    private final JLabel label;

    public BlendPanel(ImageLayer layer) {
        slider = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) (layer.getGLImage().getBlend() * 100));
        label = new JLabel(LevelsPanel.align3(slider.getValue()), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            layer.getGLImage().setBlend(slider.getValue() / 100.);
            label.setText(LevelsPanel.align3(slider.getValue())); // additivity
            Display.display();
        });
        WheelSupport.installMouseWheelSupport(slider);
    }

    @Override
    public Component getTitle() {
        return new JLabel("Blend", JLabel.RIGHT);
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
