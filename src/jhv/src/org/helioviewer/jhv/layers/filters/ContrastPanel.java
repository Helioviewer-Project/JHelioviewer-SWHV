package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayerOptions;

public class ContrastPanel implements ChangeListener, FilterDetails {

    private final JSlider slider;
    private final JLabel label;
    private final int STEP = 10;

    public ContrastPanel() {
        slider = new JSlider(JSlider.HORIZONTAL, 0, 200, 100);
        label = new JLabel(String.format("%3d%%", slider.getValue()), JLabel.RIGHT);
        slider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(slider);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setContrast(slider.getValue() / 100f);
        label.setText(String.format("%3d%%", slider.getValue()));
        Displayer.display();
    }

    @Override
    public Component getTitle() {
        return new JLabel("Contrast", JLabel.RIGHT);
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
