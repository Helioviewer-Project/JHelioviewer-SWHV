package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayerOptions;

public class SharpenPanel implements ChangeListener, FilterDetails {

    private final JSlider slider;
    private final JLabel label;

    public SharpenPanel() {
        slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        label = new JLabel(String.format("%3d%%", slider.getValue()), JLabel.RIGHT);
        slider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(slider);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setSharpen(slider.getValue() / 10f);
        label.setText(String.format("%3d%%", slider.getValue()));
        Displayer.display();
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
